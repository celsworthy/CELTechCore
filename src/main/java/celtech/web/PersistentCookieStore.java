package celtech.web;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;

/**
 *
 * @author Ian
 */
public class PersistentCookieStore implements CookieStore, Runnable
{
    CookieStore store;

    public PersistentCookieStore()
    {
        // get the default in memory cookie store
        store = new CookieManager().getCookieStore();

        // todo: read in cookies from persistant storage
        // and add them store
        
        
        // add a shutdown hook to write out the in memory cookies
        Runtime.getRuntime().addShutdownHook(new Thread(this));
    }

    @Override
    public void run()
    {
        // todo: write cookies in store to persistent storage
    }

    @Override
    public void add(URI uri, HttpCookie cookie)
    {
        store.add(uri, cookie);
    }

    @Override
    public List<HttpCookie> get(URI uri)
    {
        return store.get(uri);
    }

    @Override
    public List<HttpCookie> getCookies()
    {
        return store.getCookies();
    }

    @Override
    public List<URI> getURIs()
    {
        return store.getURIs();
    }

    @Override
    public boolean remove(URI uri, HttpCookie cookie)
    {
        return store.remove(uri, cookie);
    }

    @Override
    public boolean removeAll()
    {
        return store.removeAll();
    }
}
