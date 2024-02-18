package nostr.si4n6r;

import nostr.si4n6r.shibboleth.web.HomePage;
import nostr.si4n6r.shibboleth.web.LoginPage;
import nostr.si4n6r.shibboleth.web.SignUpPage;
import org.apache.wicket.Session;
import org.apache.wicket.csp.CSPDirective;
import org.apache.wicket.csp.CSPDirectiveSrcValue;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;

/**
 * Application object for your web application.
 * If you want to run this application without deploying, run the Start class.
 *
 */
public class WicketApplication extends WebApplication
{
	/**
	 * @see org.apache.wicket.Application#getHomePage()
	 */
	@Override
	public Class<? extends WebPage> getHomePage()
	{
		return HomePage.class;
	}

	/**
	 * @see org.apache.wicket.Application#init()
	 */
	@Override
	public void init() {
		super.init();

		// Update the CSP settings
		getCspSettings().blocking().clear()
				.add(CSPDirective.STYLE_SRC, CSPDirectiveSrcValue.SELF)
				.add(CSPDirective.STYLE_SRC, "https://fonts.googleapis.com/css")
				.add(CSPDirective.STYLE_SRC, "http://localhost:7070")
				.add(CSPDirective.FONT_SRC, "https://fonts.gstatic.com")
				.add(CSPDirective.SCRIPT_SRC, CSPDirectiveSrcValue.SELF)
				.add(CSPDirective.SCRIPT_SRC, "http://localhost:7070")
				.isNonceEnabled();


		// add your configuration here
		mountPage("/login", LoginPage.class);
		mountPage("/home", HomePage.class);
		mountPage("/register", SignUpPage.class);
	}
	@Override
	public Session newSession(Request request, Response response) {
		return new CustomWebSession(request);
	}
}
