package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.response.RevenueReportDto;
import com.re.cinemamoviebookingsystem.entity.Booking;
import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportExportService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReportService reportService;
    private final MovieDisplayService movieDisplayService;

    @Transactional(readOnly = true)
    public void writeRevenueCsv(Writer writer, int year, LocalDate from, LocalDate to) throws IOException {
        RevenueReportDto report = reportService.getReport(year, from, to);
        writer.write("Bao cao doanh thu\n");
        writer.write("Tu," + from + ",Den," + to + "\n");
        writer.write("Tong doanh thu," + report.getTotalRevenue() + "\n");
        writer.write("Ky truoc," + report.getPreviousPeriodRevenue() + "\n");
        writer.write("Thay doi %," + report.getRevenueChangePercent() + "\n");
        writer.write("Don PAID," + report.getPaidBookings() + "\n");
        writer.write("Don PENDING," + report.getPendingBookings() + "\n");
        writer.write("Don CANCELLED," + report.getCancelledBookings() + "\n\n");

        writer.write("Thang,Doanh thu\n");
        for (Map.Entry<Integer, BigDecimal> e : report.getRevenueByMonth().entrySet()) {
            writer.write(e.getKey() + "," + e.getValue() + "\n");
        }
        writer.write("\nTop phim,Ten,Doanh thu\n");
        for (RevenueReportDto.TopMovieDto m : report.getTopMovies()) {
            writer.write(csv(m.getTitle()) + "," + m.getRevenue() + "\n");
        }
        writer.write("\nTop phong,Ten,Doanh thu,So don\n");
        for (RevenueReportDto.TopRoomDto r : report.getTopRooms()) {
            writer.write(csv(r.getRoomName()) + "," + r.getRevenue() + "," + r.getBookingCount() + "\n");
        }
    }

    @Transactional(readOnly = true)
    public void writeBookingsCsv(Writer writer, LocalDate from, LocalDate to, BookingStatus status) throws IOException {
        List<Booking> bookings = reportService.listBookingsForExport(from, to, status);
        writer.write("Ma don,Trang thai,Ngay dat,Tong tien,Khach,Phim,Phong,Suat\n");
        for (Booking b : bookings) {
            String customer = b.getUser().getProfile() != null
                    ? b.getUser().getProfile().getFullName() : b.getUser().getUsername();
            String movie = movieDisplayService.resolveTitle(b.getShowtime().getMovie(), AppLanguage.VI_VN);
            writer.write(b.getBookingId() + ",");
            writer.write(b.getStatus().name() + ",");
            writer.write(b.getBookingDate().format(DT) + ",");
            writer.write(b.getTotalAmount() + ",");
            writer.write(csv(customer) + ",");
            writer.write(csv(movie) + ",");
            writer.write(csv(b.getShowtime().getRoom().getRoomName()) + ",");
            writer.write(b.getShowtime().getStartTime().format(DT) + "\n");
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
