package delta.downloads;

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.log4j.Logger;

import delta.downloads.utils.DownloadsLoggers;

/**
 * @author DAM
 */
public class RelocationFinder
{
  private static final Logger _logger=DownloadsLoggers.getDownloadsLogger();

  public String getRelocation(String urlStr)
  {
    String loc=null;
    HttpURLConnection conn=null;
    try
    {
      URL url=new URL(urlStr);
      conn=(HttpURLConnection)url.openConnection();
      conn.setInstanceFollowRedirects(false);
      conn.connect();
      System.out.println(conn.getHeaderFields());
      int responseCode=conn.getResponseCode();
      if (responseCode==302)
      {
        loc=conn.getHeaderField("Location");
        if (loc!=null)
        {
          byte[] b=loc.getBytes("ISO8859-1");
          loc=new String(b,"UTF-8");
        }
      }
    }
    catch(Exception e)
    {
      _logger.error("",e);
    }
    finally
    {
      if (conn!=null)
      {
        conn.disconnect();
      }
      conn=null;
    }
    return loc;
  }

  public static void main(String[] args)
  {
    RelocationFinder finder=new RelocationFinder();
    String url="http://content.turbine.com/sites/lorebook.lotro.com/images/icons/item/instrument/eq_c_flute_tier7_v3.png";
    String loc=finder.getRelocation(url);
    System.out.println("loc="+loc);
  }
}
