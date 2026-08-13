import { Routes } from '@angular/router';
import { AdminBannersPage } from './pages/admin-banners/admin-banners';
import { AdminCategoriesPage } from './pages/admin-categories/admin-categories';
import { AdminCoursesPage } from './pages/admin-courses/admin-courses';
import { AdminDashboardPage } from './pages/admin-dashboard/admin-dashboard';
import { AdminUsersPage } from './pages/admin-users/admin-users';
import { AuthLogin } from './pages/auth-login/auth-login';
import { AuthRegister } from './pages/auth-register/auth-register';
import { Cart } from './pages/cart/cart';
import { Catalog } from './pages/catalog/catalog';
import { Categories } from './pages/categories/categories';
import { CourseLearn } from './pages/course-learn/course-learn';
import { CourseDetail } from './pages/course-detail/course-detail';
import { Dashboard } from './pages/dashboard/dashboard';
import { Home } from './pages/home/home';
import { InstructorProfilePage } from './pages/instructor-profile/instructor-profile';
import { PerfilPage } from './pages/perfil/perfil';
import { PopupsShowcase } from './pages/popups-showcase/popups-showcase';
import { AdminPopupsPage } from './pages/admin-popups/admin-popups';
import { AdminLessons } from './pages/admin-lessons/admin-lessons';
import { Pricing } from './pages/pricing/pricing';
import { ProfesorDashboard } from './pages/profesor-dashboard/profesor-dashboard';
import { SmartTvWidget } from './pages/smart-tv-widget/smart-tv-widget';
import { UserCourses } from './pages/user-courses/user-courses';
import { WearableWidget } from './pages/wearable-widget/wearable-widget';
import { authGuard, adminGuard, guestGuard, teacherGuard, studentGuard, cursoAccesoGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: Home },
  { path: 'login', component: AuthLogin, canActivate: [guestGuard] },
  { path: 'registro', component: AuthRegister, canActivate: [guestGuard] },
  { path: 'catalogo', component: Catalog },
  { path: 'categorias', component: Categories },
  { path: 'promociones', component: Pricing },
  { path: 'precios', redirectTo: 'promociones', pathMatch: 'full' },
  { path: 'curso/:id', component: CourseDetail },
  { path: 'curso/:id/aprender', component: CourseLearn, canActivate: [authGuard, cursoAccesoGuard] },
  { path: 'carrito', component: Cart, canActivate: [authGuard] },
  { path: 'instructor/:id', component: InstructorProfilePage },
  { path: 'perfil', component: PerfilPage, canActivate: [authGuard] },
  { path: 'usuario/dashboard', component: UserCourses, canActivate: [authGuard, studentGuard] },
  { path: 'profesor/dashboard', component: ProfesorDashboard, canActivate: [authGuard, teacherGuard] },
  { path: 'admin/dashboard', component: AdminDashboardPage, canActivate: [authGuard, adminGuard] },
  { path: 'admin/cursos', component: AdminCoursesPage, canActivate: [authGuard, adminGuard] },
  { path: 'admin/categorias', component: AdminCategoriesPage, canActivate: [authGuard, adminGuard] },
  { path: 'admin/usuarios', component: AdminUsersPage, canActivate: [authGuard, adminGuard] },
  { path: 'admin/banners', component: AdminBannersPage, canActivate: [authGuard, adminGuard] },
  { path: 'admin/popups', component: AdminPopupsPage, canActivate: [authGuard, adminGuard] },
  { path: 'admin/lecciones', component: AdminLessons, canActivate: [authGuard, adminGuard] },
  { path: 'popups', component: PopupsShowcase },
  { path: 'dashboard', component: Dashboard },
  { path: 'smart-tv', component: SmartTvWidget },
  { path: 'wearable', component: WearableWidget },
  { path: '**', redirectTo: 'home' }
];
