package run.halo.gradle.steps;

import static org.apache.commons.lang3.BooleanUtils.isNotTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import run.halo.gradle.model.Condition;
import run.halo.gradle.model.Plugin;
import run.halo.gradle.utils.RetryUtils;

@Slf4j
@UtilityClass
class CheckPluginStateHelper {
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static void checkState(Supplier<Plugin> pluginSupplier) {
        RetryUtils.withRetry(20, 100, () -> {
            try {
                checkPluginState(pluginSupplier);
                return true;
            } catch (WaitPluginStartedException e) {
                return false;
            }
        });
    }

    private static void checkPluginState(Supplier<Plugin> pluginSupplier) {
        var plugin = pluginSupplier.get();
        if (plugin == null) {
            return;
        }
        if (isNotTrue(plugin.getSpec().getEnabled())) {
            return;
        }
        if (isStarted(plugin)) {
            System.out.printf("> 插件 %s（%s）已就绪! %n", plugin.getSpec().getDisplayName(),
                plugin.getMetadata().getName());
            return;
        }
        if (isFailed(plugin)) {
            printFailedReason(plugin.getStatus());
            return;
        }
        var pluginName = plugin.getMetadata().getName();
        throw new WaitPluginStartedException(
            "Waiting for plugin " + pluginName + " to start, current status: "
            + plugin.getStatus().getPhase());
    }

    private static void printFailedReason(Plugin.PluginStatus status) {
        var condition = status.getConditions().peekFirst();
        System.out.println(ANSI_RED + "> 插件启动失败!" + ANSI_RESET);
        printCondition(condition);
    }

    private static void printCondition(Condition condition) {
        if (condition == null) {
            System.out.println("No condition to display.");
            return;
        }

        var columns = new LinkedHashMap<String, Function<Condition, String>>();
        columns.put("Type", Condition::getType);
        columns.put("Reason", Condition::getReason);
        columns.put("Message", Condition::getMessage);

        // calculate the maximum width of each column
        Map<String, Integer> columnWidths = columns.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                e -> Math.max(e.getKey().length(), e.getValue().apply(condition).length()),
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));

        // print header
        columns.keySet()
            .forEach(key -> System.out.printf("%-" + (columnWidths.get(key) + 2) + "s", key));
        System.out.println();
        columns.keySet().forEach(key -> System.out.print("-".repeat(columnWidths.get(key)) + "  "));
        System.out.println();

        // print data row
        System.out.print(ANSI_RED);
        columns.forEach((key, value) -> System.out.printf("%-" + (columnWidths.get(key) + 2) + "s",
            value.apply(condition)));
        System.out.println(ANSI_RESET);
    }

    static class WaitPluginStartedException extends RuntimeException {
        public WaitPluginStartedException(String message) {
            super(message);
        }
    }

    private static boolean isStarted(Plugin plugin) {
        return plugin.getStatus().getPhase() == Plugin.Phase.STARTED;
    }

    private static boolean isFailed(Plugin plugin) {
        return plugin.getStatus().getPhase() == Plugin.Phase.FAILED;
    }
}
