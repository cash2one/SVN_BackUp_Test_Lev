import static java.lang.String.format;

public class StaticImport {
  public static void main(String[] args) {
    format("foo%s", "bar");
    <caret>java.lang.String.valueOf(true);
  }
}