package cn.icodening.console.event;

import java.util.EventListener;

/**
 * @author icodening
 * @date 2021.06.05
 */
@FunctionalInterface
public interface ConsoleEventListener<E extends AppConsoleEvent> extends EventListener {

    void onEvent(E event);
}
