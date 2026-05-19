"""Fix mistaken </motion> and <motion tags in Thymeleaf templates."""
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "templates"
CLOSE_BAD = "</" + "motion" + ">"
CLOSE_OK = "</div>"


def fix(text: str) -> str:
    text = text.replace(CLOSE_BAD, CLOSE_OK)
    text = re.sub(r"<" + "motion" + r"\b", "<div", text)
    return text


def main() -> None:
    for path in ROOT.rglob("*.html"):
        original = path.read_text(encoding="utf-8")
        updated = fix(original)
        if updated != original:
            path.write_text(updated, encoding="utf-8")
            print("fixed", path.relative_to(ROOT))


if __name__ == "__main__":
    main()
