package delta.downloads.utils;

import org.apache.log4j.Logger;

import delta.common.utils.traces.LoggersRegistry;

/**
 * Management class for all 'downloads' loggers.
 * @author DAM
 */
public abstract class DownloadsLoggers
{
  /**
   * Name of the "downloads" logger.
   */
  public static final String DOWNLOADS="UTILS.DOWNLOADS";

  private static final Logger _downloadsLogger=LoggersRegistry.getLogger(DOWNLOADS);

  /**
   * Get the logger used for downloads (DOWNLOADS).
   * @return the logger used for downloads.
   */
  public static Logger getDownloadsLogger()
  {
    return _downloadsLogger;
  }
}
