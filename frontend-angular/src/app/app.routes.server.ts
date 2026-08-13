import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    path: 'curso/:id',
    renderMode: RenderMode.Server
  },
  {
    path: 'curso/:id/aprender',
    renderMode: RenderMode.Server
  },
  {
    path: 'instructor/:id',
    renderMode: RenderMode.Server
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender
  }
];
