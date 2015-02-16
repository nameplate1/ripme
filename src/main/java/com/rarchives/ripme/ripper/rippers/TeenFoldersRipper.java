package com.rarchives.ripme.ripper.rippers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.ripper.DownloadThreadPool;
import com.rarchives.ripme.utils.Http;
import com.rarchives.ripme.utils.Utils;

public class TeenfoldersRipper extends AbstractHTMLRipper {


	public TeenfoldersRipper( URL url ) throws IOException {
		super( url );
	}
	
	@Override
	public String getHost() {
		return "teenfolders";
	}
	
	@Override
	public String getDomain() {
		return "teenfolders.com";
	}
	
	@Override
	public String getGID( URL url ) throws MalformedURLException {
		Pattern p = Pattern.compile( "^https?://[w\\.]\{0,4\}teenfolders\\.com/galleries/([a-zA-Z0-9]+).*$" );
		Matcher m = p.matcher( url.toExternalForm() );
		if ( m.matches() ) {
			// Return the text contained between () in the regex
			return m.group( 1 );
		}
		throw new MalformedURLException( "Expected teenfolders.com URL format: " +
			"teenfolders.com/galleries/albumid/albumname - got " + url + " instead" );
	}

	@Override
	public Document getFirstPage() throws IOException {
		// "url" is an instance field of the superclass
		return Http.url( url ).get();
	}

	@Override
	public Document getNextPage( Document doc ) throws IOException {
	
		// Find next page
		Elements hrefs = doc.select( "a.pagination_current + a.pagination_link" );
		if ( hrefs.size() == 0 ) {
			throw new IOException( "No more pages" );
		}
		
		String nextUrl = "http://www.imagebam.com" + hrefs.first().attr( "href" );
		
		sleep( 500 );
		
		return Http.url( nextUrl ).get();
	}

	@Override
	public List<String> getURLsFromPage( Document doc ) {
		List<String> imageURLs = new ArrayList<String>();
		for ( Element thumb : doc.select( "div > a[target=_blank]:not(.footera)" ) ) {
			imageURLs.add( thumb.attr( "href" ) );
		}
		return imageURLs;
	}

	@Override
	public void downloadURL( URL url, int index ) {
		ImagebamImageThread t = new ImagebamImageThread( url, index );
		imagebamThreadPool.addThread( t );
		sleep( 500 );
	}

	@Override
	public String getAlbumTitle( URL url ) throws MalformedURLException {
		try {
			// Attempt to use album title as GID
			Elements elems = getFirstPage().select( "legend" );
			String title = elems.first().text();
			logger.info( "Title text: '" + title + "'" );
			Pattern p = Pattern.compile( "^(.*)\\s\\d* image.*$" );
			Matcher m = p.matcher( title );
			if ( m.matches() ) {
				return getHost() + "_" + getGID( url ) + " ( " + m.group( 1 ).trim() + " )";
			}
			logger.info( "Doesn't match " + p.pattern() );
		} catch ( Exception e ) {
			// Fall back to default album naming convention
			logger.warn( "Failed to get album title from " + url, e );
		}
		return super.getAlbumTitle( url );
	}
	
}
