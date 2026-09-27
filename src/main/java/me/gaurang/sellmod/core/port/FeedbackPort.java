package me.gaurang.sellmod.core.port;

/** Non-blocking user feedback (system toasts). */
public interface FeedbackPort {
    void toast(String title, String message);
}
