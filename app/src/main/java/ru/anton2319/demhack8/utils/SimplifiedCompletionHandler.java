package ru.anton2319.demhack8.utils;

/**
 * Pays no attention to messages or overloads unless overridden explicitly
 */
public abstract class SimplifiedCompletionHandler implements CompletionHandler {
    @Override
    public void handle(String message) {
        handle();
    }

    @Override
    public void handle(Exception exception) {
        exception.printStackTrace();
    }

    @Override
    public void handle(Throwable throwable) {
        handle((Exception) throwable);
    }
}
