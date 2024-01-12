package nostr.si4n6r.shibboleth.web;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.Link;

import java.io.Serial;

public class HomePage extends WebPage {
	@Serial
	private static final long serialVersionUID = 1L;

	public HomePage() {
		add(new Link<Void>("loginLink") {
			@Override
			public void onClick() {
				setResponsePage(LoginPage.class);
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