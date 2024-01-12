package nostr.si4n6r.shibboleth.web;

import nostr.si4n6r.shibboleth.web.components.SignUpForm;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;

import java.io.Serial;

public class SignUpPage extends WebPage {
	@Serial
	private static final long serialVersionUID = 1L;

	public SignUpPage() {
		Form<?> form = new SignUpForm("signUpForm");
		add(form);
	}
}
