export interface Course {
  id: number;
  title: string;
  category: string;
  instructor: string;
  instructorId?: string | null;
  description?: string | null;
  rating: number;
  reviews: number;
  price: number;
  oldPrice?: number;
  level: string;
  image: string;
  progress?: number;
}

export interface BannerRow {
  title: string;
  subtitle?: string;
  image: 'green' | 'purple' | 'dark' | 'photo';
}

export function courseImagePath(image: string): string {
  if (/^(data:image\/|https?:\/\/|\/)/.test(image)) {
    return image;
  }

  const images: Record<string, string> = {
    excel: '/assets/images/course-excel.svg',
    code: '/assets/images/course-code.svg',
    laptop: '/assets/images/course-laptop.svg',
    screen: '/assets/images/course-dashboard.svg',
    design: '/assets/images/course-design.svg',
    purple: '/assets/images/course-purple.svg',
    banner: '/assets/images/course-code.svg'
  };

  return images[image] ?? images['code'];
}

export function bannerImagePath(image: BannerRow['image']): string {
  const images: Record<BannerRow['image'], string> = {
    green: '/assets/images/banner-green.svg',
    purple: '/assets/images/banner-purple.svg',
    dark: '/assets/images/course-dashboard.svg',
    photo: '/assets/images/promo-instructor.svg'
  };

  return images[image];
}
