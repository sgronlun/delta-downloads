package delta.downloads.async;

/**
 * Listener for download status.
 * @author DAM
 */
public interface DownloadListener
{
  /**
   * Invoked when a download task has been updated.
   * @param task Updated task.
   */
  void downloadTaskUpdated(DownloadTask task);
}
