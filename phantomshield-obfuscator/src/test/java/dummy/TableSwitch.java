package dummy;

public class TableSwitch {
    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            switch (i) {
                case -1:
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    System.out.println("smaller than 7");
                case 8:
                case 9:
                case 10:
                case 12:
                case 13:
                    System.out.println("-1 ~ 13");
                    break;
                case 14:
                case 15:
                    System.out.println("14,15");
                    break;
                default:
                    System.out.println("default");
                    break;

            }
        }
    }
}
