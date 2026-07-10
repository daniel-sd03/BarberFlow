package sodresoftwares.barbearia.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HomeController {
	
	@PostMapping("/home")
	public ResponseEntity<Void> homePage() {
		 return ResponseEntity.ok().build();
	}
}
