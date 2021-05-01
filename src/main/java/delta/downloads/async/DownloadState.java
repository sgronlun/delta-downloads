package delta.downloads.async;

/**
 * Download state.
 * @author DAM
 */
public enum DownloadState
{
  /**
   * Not running/started.
   */
  NOT_RUNNING,
  /**
   * Running.
   */
  RUNNING,
  /**
   * Terminated OK.
   */
  OK,
  /**
   * Failed.
   */
  FAILED,
  /**
   * Cancelled.
   */
  CANCELLED
}
