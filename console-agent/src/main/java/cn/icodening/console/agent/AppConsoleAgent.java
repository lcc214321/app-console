package cn.icodening.console.agent;

import cn.icodening.console.AgentStartEventProvider;
import cn.icodening.console.boot.BootServiceManager;
import cn.icodening.console.event.AgentStartEvent;
import cn.icodening.console.event.EventDispatcher;
import cn.icodening.console.extension.ExtensionClassLoader;
import cn.icodening.console.injector.ClasspathInjector;
import cn.icodening.console.injector.ClasspathRegistry;
import cn.icodening.console.logger.Logger;
import cn.icodening.console.logger.LoggerFactory;
import cn.icodening.console.util.ExtensionClassLoaderHolder;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author icodening
 * @date 2021.05.20
 */
public class AppConsoleAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConsoleAgent.class);

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        addRequiredDependency();
        List<AgentStartEvent> agentStartEvents = getAgentStartEventList();
        if (agentStartEvents.isEmpty()) {
            //直接启动
            startAgent(agentArgs, instrumentation);
            return;
        }
        //回调启动
        EventDispatcher.registerOnceEvent(agentStartEvents.get(0).getClass(), event -> {
            ((AgentStartEvent) event).invoke(agentArgs, instrumentation);
            startAgent(agentArgs, instrumentation);
        });
    }

    private static List<AgentStartEvent> getAgentStartEventList() {
        ExtensionClassLoader classLoader = ExtensionClassLoaderHolder.get();
        ServiceLoader<AgentStartEventProvider> load = ServiceLoader.load(AgentStartEventProvider.class, classLoader);
        Iterator<AgentStartEventProvider> iterator = load.iterator();
        List<AgentStartEvent> agentStartEvents = new ArrayList<>(4);
        iterator.forEachRemaining(event -> {
            if (event.get() != null) {
                agentStartEvents.add(event.get());
            }
        });
        return agentStartEvents;
    }

    private static void startAgent(String agentArgs, Instrumentation instrumentation) {
        LOGGER.info("app console agent start, version is v" + AppConsoleAgent.class.getPackage().getImplementationVersion());
        // 启动所有服务扩展点
        try {
            BootServiceManager.initBootServices(agentArgs);
            BootServiceManager.startBootServices();
        } catch (Exception e) {
            LOGGER.warn(e.getMessage(), e);
        }

        // TODO Extension

        // 安全销毁所有服务扩展点
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                BootServiceManager.destroyBootServices();
            } catch (Exception e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }));
    }

    private static void addRequiredDependency() {
        ExtensionClassLoader classLoader = ExtensionClassLoaderHolder.get();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ServiceLoader<ClasspathInjector> load = ServiceLoader.load(ClasspathInjector.class, classLoader);
        Iterator<ClasspathInjector> iterator = load.iterator();
        ArrayList<ClasspathInjector> classPathConfigurers = new ArrayList<>();
        while (iterator.hasNext()) {
            classPathConfigurers.add(iterator.next());
        }
        ClasspathRegistry classPathRegistry = new ClasspathRegistry();
        for (ClasspathInjector classPathConfigurer : classPathConfigurers) {
            String jarPathByClass = classLoader.getJarPathByClass(classPathConfigurer.getClass().getName().replace('.', '/').concat(".class"));
            if (jarPathByClass != null
                    && classPathConfigurer.shouldInject()) {
                classPathRegistry.addUrl("file:" + jarPathByClass);
            }
        }
        try {
            Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrlMethod.setAccessible(true);
            List<String> allUrls = classPathRegistry.getAllUrl();
            for (String u : allUrls) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(u);
                }
                URL url = new URL(u);
                addUrlMethod.invoke(contextClassLoader, url);
            }
            if (allUrls.isEmpty()) {
                LOGGER.warn("no jar to add to the class path");
            }
        } catch (Exception e) {
            LOGGER.warn(e);
        }
    }
}
