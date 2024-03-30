package ru.anton2319.demhack8.utils;

public interface CompletionHandler {
    /**
     * All went fine
     */
    public void handle();
    /**
     * All went fine, but you want to provide a description or a warning message
     * @param message
     */
    public void handle(String message);

    /**
     * Handle the exception that might have occurred during the execution
     * @param exception
     */
    public void handle(Exception exception);

    /**
     * Throwable error handler overload
     * @param throwable
     */
    public void handle(Throwable throwable);
}
