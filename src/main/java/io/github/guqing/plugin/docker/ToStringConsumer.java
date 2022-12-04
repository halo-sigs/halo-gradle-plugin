package io.github.guqing.plugin.docker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ToStringConsumer extends BaseConsumer<ToStringConsumer> {

    @Override
    public void accept(OutputFrame outputFrame) {
        OutputFrame.OutputType outputType = outputFrame.getType();

        String utf8String = outputFrame.getUtf8String();
        utf8String = FrameConsumerResultCallback.LINE_BREAK_AT_END_PATTERN.matcher(utf8String)
            .replaceAll("");
        switch (outputType) {
            case END:
                break;
            case STDOUT:
                System.out.println(utf8String);
                break;
            case STDERR:
                System.out.println("\033[31;m" + utf8String + "\033[0m");
                break;
            default:
                throw new IllegalArgumentException("Unexpected outputType " + outputType);
        }
    }
}
