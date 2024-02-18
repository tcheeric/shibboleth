package nostr.si4n6r.shibboleth.web;

import nostr.si4n6r.ApplicationConfiguration;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serial;

public class HomePage extends WebPage {
	@Serial
	private static final long serialVersionUID = 1L;

	public HomePage() {
		add(new Link<Void>("loginLink") {
			@Override
			public void onClick() {
				PageParameters params = new PageParameters();
				params.add("app", ApplicationConfiguration.getInstance().getAppNpub()); // replace "yourAppValue" with the actual value
				setResponsePage(LoginPage.class, params);
			}
		});

		add(new Link<Void>("registerLink") {
			@Override
			public void onClick() {
				setResponsePage(SignUpPage.class);
			}
		});
	}
}