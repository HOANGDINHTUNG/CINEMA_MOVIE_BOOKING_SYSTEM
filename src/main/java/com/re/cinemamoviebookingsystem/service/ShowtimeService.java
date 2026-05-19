package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.config.CinemaProperties;
import com.re.cinemamoviebookingsystem.dto.request.ShowtimeCreateRequest;
import com.re.cinemamoviebookingsystem.dto.response.*;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.util.MovieAgeUtil;
import com.re.cinemamoviebookingsystem.entity.*;
import com.re.cinemamoviebookingsystem.enums.SeatStatus;
import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeStatusService showtimeStatusService;
    private final CinemaProperties cinemaProperties;
    private final MovieDisplayService movieDisplayService;

    private static final DateTimeFormatter TAB_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter SLOT_TIME = DateTimeFormatter.ofPattern("HH:mm");

    @Transactional(rollbackFor = Exception.class)
    public Long createShowtime(ShowtimeCreateRequest request) {
        Movie movie = resolveMovie(request);
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Phòng không tồn tại"));

        LocalDateTime start = request.getStartTime();
        LocalDateTime end = start.plusMinutes(movie.getDuration())
                .plusMinutes(cinemaProperties.getCleaningBufferMinutes());

        if (showtimeRepository.existsRoomConflict(room.getRoomId(), start, end, null)) {
            throw new BusinessException(ErrorCode.ROOM_CONFLICT,
                    "Phòng đã có suất chiếu trùng lịch. Vui lòng chọn giờ khác.");
        }

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .room(room)
                .startTime(start)
                .endTime(end)
                .basePrice(request.getBasePrice())
                .status(ShowtimeStatus.ACTIVE)
                .build();
        showtime = showtimeRepository.save(showtime);

        List<Seat> seats = seatRepository.findByRoomRoomId(room.getRoomId());
        List<ShowtimeSeat> showtimeSeats = new ArrayList<>();
        for (Seat seat : seats) {
            showtimeSeats.add(ShowtimeSeat.builder()
                    .showtime(showtime)
                    .seat(seat)
                    .status(SeatStatus.AVAILABLE)
                    .build());
        }
        showtimeSeatRepository.saveAll(showtimeSeats);
        return showtime.getShowtimeId();
    }

    @Transactional(readOnly = true)
    public ScheduleViewDto buildScheduleView(LocalDate selectedDate) {
        LocalDate today = LocalDate.now();
        List<LocalDate> dayList = IntStream.range(0, 5).mapToObj(today::plusDays).toList();
        LocalDate selected = (selectedDate != null && dayList.contains(selectedDate)) ? selectedDate : today;

        LocalDate finalSelected = selected;
        Map<Long, List<ShowtimeBrowseDto>> grouped = browseUpcoming().stream()
                .filter(s -> s.getTmdbId() != null)
                .filter(s -> s.getStartTime().toLocalDate().equals(finalSelected))
                .collect(Collectors.groupingBy(ShowtimeBrowseDto::getTmdbId));

        List<ScheduleDayTabDto> tabs = dayList.stream()
                .map(d -> ScheduleDayTabDto.builder()
                        .date(d)
                        .label(d.format(TAB_DATE))
                        .selected(d.equals(selected))
                        .build())
                .toList();

        List<ScheduleMovieCardDto> cards = new ArrayList<>();
        for (Map.Entry<Long, List<ShowtimeBrowseDto>> entry : grouped.entrySet()) {
            Long tmdbId = entry.getKey();
            ShowtimeBrowseDto first = entry.getValue().get(0);
            List<ScheduleSlotDto> slotDtos = entry.getValue().stream()
                    .sorted(Comparator.comparing(ShowtimeBrowseDto::getStartTime))
                    .map(s -> ScheduleSlotDto.builder()
                            .showtimeId(s.getShowtimeId())
                            .timeLabel(s.getStartTime().format(SLOT_TIME))
                            .soldOut(s.isSoldOut())
                            .build())
                    .toList();
            cards.add(ScheduleMovieCardDto.builder()
                    .tmdbId(tmdbId)
                    .movieId(first.getMovieId())
                    .title(first.getMovieTitle())
                    .posterUrl(null)
                    .genresLabel("—")
                    .duration(null)
                    .releaseDate(null)
                    .ageLabel(resolveAgeLabel(first))
                    .ageNote(MovieAgeUtil.buildAgeNote(first.getMovieTitle()))
                    .format("2D")
                    .slots(slotDtos)
                    .build());
        }
        cards.sort(Comparator.comparing(ScheduleMovieCardDto::getTitle, String.CASE_INSENSITIVE_ORDER));

        return ScheduleViewDto.builder()
                .days(tabs)
                .selectedDate(selected)
                .movies(cards)
                .filtering(false)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<Long, LocalDateTime> mapNextShowtimeByTmdbId() {
        LocalDateTime now = LocalDateTime.now();
        List<ShowtimeStatus> statuses = List.of(ShowtimeStatus.ACTIVE, ShowtimeStatus.SOLD_OUT);
        Map<Long, LocalDateTime> nextByTmdb = new LinkedHashMap<>();
        for (Showtime showtime : showtimeRepository.findUpcoming(now, statuses)) {
            Long tmdbId = showtime.getMovie().getTmdbId();
            if (tmdbId == null) {
                continue;
            }
            LocalDateTime start = showtime.getStartTime();
            nextByTmdb.merge(tmdbId, start, (a, b) -> a.isBefore(b) ? a : b);
        }
        return nextByTmdb;
    }

    @Transactional(readOnly = true)
    public List<ShowtimeBrowseDto> browseUpcoming() {
        LocalDateTime now = LocalDateTime.now();
        List<ShowtimeStatus> statuses = List.of(ShowtimeStatus.ACTIVE, ShowtimeStatus.SOLD_OUT);
        return showtimeRepository.findUpcoming(now, statuses).stream()
                .map(this::toBrowseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Showtime> listAllForAdmin() {
        return showtimeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ShowtimeBrowseDto> listByMovie(Long movieId) {
        LocalDateTime now = LocalDateTime.now();
        return showtimeRepository.findUpcoming(now, List.of(ShowtimeStatus.ACTIVE, ShowtimeStatus.SOLD_OUT))
                .stream()
                .filter(s -> s.getMovie().getMovieId().equals(movieId))
                .map(this::toBrowseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShowtimeBrowseDto> listByTmdbId(long tmdbId) {
        return browseUpcoming().stream()
                .filter(s -> s.getTmdbId() != null && s.getTmdbId() == tmdbId)
                .sorted(Comparator.comparing(ShowtimeBrowseDto::getStartTime))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShowtimeDayGroupDto> groupShowtimesByDay(List<ShowtimeBrowseDto> showtimes, Locale locale) {
        if (showtimes == null || showtimes.isEmpty()) {
            return List.of();
        }
        Locale effective = locale != null ? locale : Locale.forLanguageTag("vi-VN");
        DateTimeFormatter dayLabel = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", effective);
        Map<LocalDate, List<ShowtimeBrowseDto>> grouped = showtimes.stream()
                .collect(Collectors.groupingBy(s -> s.getStartTime().toLocalDate(), TreeMap::new, Collectors.toList()));
        List<ShowtimeDayGroupDto> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<ShowtimeBrowseDto>> entry : grouped.entrySet()) {
            LocalDate date = entry.getKey();
            List<ShowtimeBrowseDto> slots = entry.getValue().stream()
                    .sorted(Comparator.comparing(ShowtimeBrowseDto::getStartTime))
                    .collect(Collectors.toList());
            result.add(ShowtimeDayGroupDto.builder()
                    .date(date)
                    .dateLabel(date.format(dayLabel))
                    .monthLabel(formatScheduleMonth(date, effective))
                    .dayNumber(String.valueOf(date.getDayOfMonth()))
                    .weekdayLabel(formatScheduleWeekday(date, effective))
                    .slots(slots)
                    .build());
        }
        return result;
    }

    private static String formatScheduleMonth(LocalDate date, Locale locale) {
        if ("vi".equalsIgnoreCase(locale.getLanguage())) {
            return "Th. " + date.format(DateTimeFormatter.ofPattern("MM"));
        }
        return date.format(DateTimeFormatter.ofPattern("MMM", locale));
    }

    private static String formatScheduleWeekday(LocalDate date, Locale locale) {
        return date.format(DateTimeFormatter.ofPattern("EEEE", locale));
    }

    @Transactional(readOnly = true)
    public SeatMapDto getSeatMap(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOWTIME_NOT_FOUND, "Suất chiếu không tồn tại"));

        showtimeStatusService.refreshShowtimeStatus(showtimeId);
        showtime = showtimeRepository.findById(showtimeId).orElseThrow();

        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeShowtimeId(showtimeId);
        long available = seats.stream()
                .filter(s -> s.getStatus() == SeatStatus.AVAILABLE
                        || (s.getStatus() == SeatStatus.LOCKED && isLockExpired(s)))
                .count();

        return SeatMapDto.builder()
                .showtimeId(showtimeId)
                .movieId(showtime.getMovie().getMovieId())
                .tmdbId(showtime.getMovie().getTmdbId())
                .movieTitle(movieDisplayService.resolveTitle(showtime.getMovie(), AppLanguage.VI_VN))
                .roomName(showtime.getRoom().getRoomName())
                .startTime(showtime.getStartTime())
                .basePrice(showtime.getBasePrice())
                .showtimeStatus(showtime.getStatus())
                .soldOut(showtime.getStatus() == ShowtimeStatus.SOLD_OUT)
                .seats(seats.stream().map(ss -> SeatMapDto.SeatCellDto.builder()
                        .seatId(ss.getSeat().getSeatId())
                        .label(ss.getSeat().getLabel())
                        .seatType(ss.getSeat().getSeatType().name())
                        .status(displayStatus(ss))
                        .build()).collect(Collectors.toList()))
                .build();
    }

    public BigDecimal estimateSeatTotal(SeatMapDto seatMap, List<Long> seatIds) {
        if (seatMap == null || seatIds == null || seatIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return seatMap.getSeats().stream()
                .filter(s -> seatIds.contains(s.getSeatId()))
                .map(s -> {
                    if ("VIP".equalsIgnoreCase(s.getSeatType())) {
                        return seatMap.getBasePrice()
                                .multiply(BigDecimal.valueOf(cinemaProperties.getVipPriceMultiplier()))
                                .setScale(0, RoundingMode.HALF_UP);
                    }
                    return seatMap.getBasePrice();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private SeatStatus displayStatus(ShowtimeSeat ss) {
        if (ss.getStatus() == SeatStatus.LOCKED && isLockExpired(ss)) {
            return SeatStatus.AVAILABLE;
        }
        return ss.getStatus();
    }

    private boolean isLockExpired(ShowtimeSeat ss) {
        return ss.getLockedUntil() != null && ss.getLockedUntil().isBefore(LocalDateTime.now());
    }

    private Movie resolveMovie(ShowtimeCreateRequest request) {
        if (request.getTmdbId() != null) {
            return movieRepository.findByTmdbId(request.getTmdbId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST,
                            "Chưa có phim rạp với TMDB id=" + request.getTmdbId()
                                    + ". Import phim từ TMDB trước khi tạo suất chiếu."));
        }
        if (request.getMovieId() != null) {
            return movieRepository.findById(request.getMovieId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Phim không tồn tại"));
        }
        throw new BusinessException(ErrorCode.INVALID_REQUEST, "Cần TMDB id hoặc movie id");
    }

    private static String resolveAgeLabel(ShowtimeBrowseDto first) {
        if (first.getAgeLabel() != null && !first.getAgeLabel().isBlank()) {
            return first.getAgeLabel();
        }
        return MovieAgeUtil.extractAgeLabel(first.getMovieTitle());
    }

    private ShowtimeBrowseDto toBrowseDto(Showtime s) {
        long booked = showtimeSeatRepository.countByShowtimeShowtimeIdAndStatus(
                s.getShowtimeId(), SeatStatus.BOOKED);
        int total = s.getRoom().getTotalSeats();
        boolean soldOut = s.getStatus() == ShowtimeStatus.SOLD_OUT || booked >= total;
        Movie movie = s.getMovie();
        return ShowtimeBrowseDto.builder()
                .showtimeId(s.getShowtimeId())
                .movieId(movie.getMovieId())
                .tmdbId(movie.getTmdbId())
                .ageLabel(movie.getAgeLabel())
                .movieTitle(movieDisplayService.resolveTitle(movie, AppLanguage.VI_VN))
                .roomName(s.getRoom().getRoomName())
                .startTime(s.getStartTime())
                .basePrice(s.getBasePrice())
                .status(s.getStatus())
                .soldOut(soldOut)
                .availableSeats(total - booked)
                .totalSeats(total)
                .build();
    }
}
