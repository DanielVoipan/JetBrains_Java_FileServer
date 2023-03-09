import java.util.*;

class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String[] str = scanner.nextLine().split("");
        Deque<Character> stack = new ArrayDeque<>();
        Map<String, String> map = new HashMap<>();
        map.put("{", "}");
        map.put("[", "]");
        map.put("(", ")");
        for (String s : str) {
            if (map.containsKey(s)) {
                stack.push(s.charAt(0));
            } else if (map.containsValue(s)) {
                if (stack.isEmpty()) {
                    System.out.println(false);
                    return;
                }
                for (var i : map.entrySet()) {
                    if (i.getValue().equals(s) && i.getKey().equals(stack.getFirst().toString())) {
                        stack.pop();
                        break;
                    }
                }
            }
        }
        if (stack.isEmpty()) {
            System.out.println(true);
        } else {
            System.out.println(false);
        }
    }
}
