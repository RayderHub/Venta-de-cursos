import { createClient, SupabaseClient, User, Session } from '@supabase/supabase-js';
import { isPlatformBrowser } from '@angular/common';
import { environment } from '../../../environments/environment';
import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';

export interface Profile {
  id: string;
  email: string;
  full_name: string | null;
  avatar_url: string | null;
  bio: string | null;
  role_id: string;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

export interface Role {
  id: string;
  name: string;
  description: string | null;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private supabase: SupabaseClient | null = null;
  private router = inject(Router);
  private platformId = inject(PLATFORM_ID);
  private authReadyResolver: (() => void) | null = null;
  private readonly authReady = new Promise<void>((resolve) => {
    this.authReadyResolver = resolve;
  });

  readonly user = signal<User | null>(null);
  readonly profile = signal<Profile | null>(null);
  readonly session = signal<Session | null>(null);
  readonly loading = signal(true);

  readonly isAuthenticated = computed(() => !!this.session());
  readonly isAdmin = computed(() => this.profile()?.role_id === 'admin');
  readonly isTeacher = computed(() => this.profile()?.role_id === 'teacher');
  readonly userRole = computed(() => this.profile()?.role_id ?? 'student');

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.supabase = createClient(environment.supabaseUrl, environment.supabaseAnonKey);
      this.initAuth();
    } else {
      this.markAuthReady();
    }
  }

  private initAuth() {
    if (!this.supabase) {
      this.markAuthReady();
      return;
    }

    void this.supabase.auth.getSession()
      .then(async ({ data: { session } }) => {
        this.session.set(session);
        this.user.set(session?.user ?? null);

        if (session?.user) {
          await this.loadProfile(session.user.id);
        } else {
          this.profile.set(null);
        }
      })
      .catch(() => {
        this.session.set(null);
        this.user.set(null);
        this.profile.set(null);
      })
      .finally(() => {
        this.markAuthReady();
      });

    this.supabase.auth.onAuthStateChange(async (event, session) => {
      this.session.set(session);
      this.user.set(session?.user ?? null);

      if (session?.user) {
        await this.loadProfile(session.user.id);
      } else {
        this.profile.set(null);
      }

      if (event === 'SIGNED_OUT') {
        await this.router.navigate(['/home']);
      }
    });
  }

  private markAuthReady(): void {
    this.loading.set(false);
    this.authReadyResolver?.();
    this.authReadyResolver = null;
  }

  async waitUntilReady(): Promise<void> {
    await this.authReady;
  }

  getHomeRouteForRole(roleId: string | null | undefined = this.userRole()): string {
    if (roleId === 'admin') return '/admin/dashboard';
    if (roleId === 'teacher') return '/profesor/dashboard';
    return '/usuario/dashboard';
  }

  async loadProfile(userId: string): Promise<Profile | null> {
    if (!this.supabase) return null;
    const { data, error } = await this.supabase
      .from('profiles')
      .select('*')
      .eq('id', userId)
      .single();

    if (!error && data) {
      this.profile.set(data);
      return data as Profile;
    }

    this.profile.set(null);
    return null;
  }

  async signUp(email: string, password: string, fullName: string) {
    if (!this.supabase) return { data: null, error: new Error('Supabase no disponible') };
    const { data, error } = await this.supabase.auth.signUp({
      email,
      password,
      options: {
        data: {
          full_name: fullName,
          role_id: 'student'
        }
      }
    });
    return { data, error };
  }

  async signIn(email: string, password: string) {
    if (!this.supabase) return { data: null, error: new Error('Supabase no disponible') };
    const { data, error } = await this.supabase.auth.signInWithPassword({
      email,
      password
    });

    if (!error && data.user) {
      await this.loadProfile(data.user.id);
    }
    return { data, error };
  }

  async signOut() {
    if (!this.supabase) return { error: null };
    const { error } = await this.supabase.auth.signOut();
    return { error };
  }

  async updateProfile(updates: Partial<Profile>) {
    const user = this.user();
    if (!user || !this.supabase) return { error: new Error('No user') };

    const { data, error } = await this.supabase
      .from('profiles')
      .update(updates)
      .eq('id', user.id)
      .select()
      .single();

    if (!error && data) {
      this.profile.set(data);
    }
    return { data, error };
  }

  getSupabase() {
    return this.supabase;
  }
}
