package ro.ubm.sd.common;

import ro.ubm.sd.common.Discovery.State;

public class DiscoveryCheck {
    public static void main(String[] args) {
        check(Discovery.stateFor(0) == State.ACTIV, "HELLO proaspat: ACTIV");
        check(Discovery.stateFor(Discovery.HELLO_MS + 500) == State.ACTIV, "HELLO intarziat putin: inca ACTIV");
        check(Discovery.stateFor(Discovery.SUSPECT_MS + 1) == State.SUSPECT, "un HELLO ratat: SUSPECT");
        check(Discovery.stateFor(Discovery.DEAD_MS + 1) == State.CAZUT, "doua HELLO ratate: CAZUT");
        check(Discovery.DEAD_MS + Discovery.CHECK_MS <= 5000, "detectare in max 5 s");
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError(name);
        }
        System.out.println("OK: " + name);
    }
}
