import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Scanner;

class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int nr = scanner.nextInt();
        Deque<Integer> stack = new ArrayDeque<>();
        for (int i = 0; i < nr; i++) {
            int n = scanner.nextInt();
            stack.add(n);
        }
        for (int i = 0; i < nr; i++) {
            System.out.println(stack.pollLast());
        }
    }
}
