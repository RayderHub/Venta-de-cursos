import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-public-footer',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './public-footer.html',
  styleUrl: './public-footer.css'
})
export class PublicFooter {
  readonly auth = inject(AuthService);
  readonly currentYear = new Date().getFullYear();
  readonly privacyOpen = signal(false);
  readonly termsOpen = signal(false);
}
