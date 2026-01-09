# ChitUI WordPress Theme

A modern dark WordPress theme inspired by ChitUI - a 3D Printer Control Interface. Features a sophisticated dark design with red accents, sidebar navigation, and card-based layouts perfect for tech blogs, portfolios, and project showcases.

## Theme Features

### Design
- **Dark Theme** with customizable accent colors (#ff4757 red by default)
- **Responsive Layout** that works on all devices
- **Card-Based UI** with hover effects and smooth transitions
- **Fixed Header** with collapsible sidebar navigation
- **Modern Typography** using system font stack

### Layout Components
- Fixed top navigation bar (60px height)
- Collapsible sidebar (280px width) with pinning functionality
- Responsive main content area
- Three footer widget areas
- Mobile-optimized with slide-out sidebar drawer

### WordPress Features
- Full WordPress customizer support
- Custom logo support
- Navigation menu support (Primary & Footer)
- Widget-ready sidebar and footer areas
- Featured images/thumbnails
- Custom image sizes
- Threaded comments
- Post formats support
- Sticky posts support
- Tag and category support
- Breadcrumb navigation
- Custom pagination

### Custom Components
- Status badges (success, warning, info, accent)
- Progress bars with ChitUI styling
- Custom tabs with bottom border indicators
- Icon buttons and action buttons
- Upload area with drag-and-drop styling
- File manager table styles
- Camera/image containers with aspect ratio
- Storage gauge displays
- Printer card styles for sidebar lists

## Installation

1. Download the theme files
2. Upload the `chitui-theme` folder to `/wp-content/themes/` directory
3. Activate the theme through the WordPress admin panel (Appearance > Themes)
4. Configure the theme settings in the WordPress Customizer

## Theme Setup

### Recommended Plugins
- **Contact Form 7** - For contact forms
- **Yoast SEO** - For search engine optimization
- **WP Super Cache** - For performance optimization

### Menu Setup
1. Go to Appearance > Menus
2. Create a new menu
3. Assign it to "Primary Menu" location for sidebar navigation
4. Assign another menu to "Footer Menu" for footer links

### Widget Areas
The theme includes 4 widget areas:
- **Primary Sidebar** - Main sidebar widgets
- **Footer Widget Area 1** - Left footer column
- **Footer Widget Area 2** - Center footer column
- **Footer Widget Area 3** - Right footer column

### Customizer Options
Go to Appearance > Customize to access:
- **Site Identity** - Logo, site title, tagline
- **Colors** - Accent color, success color
- **Menus** - Navigation menus
- **Widgets** - Widget areas
- **Homepage Settings** - Set your homepage

## File Structure

```
chitui-theme/
├── css/
│   ├── custom.css              # Additional custom styles
│   └── editor-style.css        # Editor styles (optional)
├── js/
│   ├── navigation.js           # Sidebar toggle & menu functionality
│   └── main.js                 # Interactive features
├── template-parts/
│   ├── content.php             # Default post content template
│   ├── content-single.php      # Single post content
│   ├── content-page.php        # Page content
│   └── content-none.php        # No content found
├── assets/
│   └── images/                 # Theme images
├── 404.php                     # 404 error page
├── archive.php                 # Archive pages
├── comments.php                # Comments template
├── footer.php                  # Footer template
├── functions.php               # Theme functions
├── header.php                  # Header template
├── index.php                   # Main template
├── page.php                    # Page template
├── sidebar.php                 # Sidebar template
├── single.php                  # Single post template
├── style.css                   # Main stylesheet with theme header
└── README.md                   # This file
```

## Color Palette

The theme uses CSS variables for easy customization:

```css
--accent-color: #ff4757       /* Primary accent (red) */
--accent-hover: #ff3838       /* Accent hover state */
--success-color: #06d6a0      /* Success indicators (teal) */
--warning-color: #ffd166      /* Warnings (yellow) */
--info-color: #118ab2         /* Information (blue) */
--bg-dark: #1a1a1a           /* Main background */
--bg-darker: #0d0d0d         /* Darker background */
--bg-card: #2d2d2d           /* Card backgrounds */
--text-primary: #ffffff       /* Primary text */
--text-secondary: #b0b0b0    /* Secondary text */
--border-color: #404040       /* Borders */
```

## Customization

### Changing Colors
Edit the CSS variables in `style.css` or use the WordPress Customizer (Appearance > Customize > ChitUI Colors).

### Adding Custom Styles
Add your custom CSS to `css/custom.css` or use the WordPress Customizer (Additional CSS).

### Child Theme
For extensive customizations, create a child theme:

1. Create a new folder: `chitui-theme-child`
2. Create `style.css`:
```css
/*
Theme Name: ChitUI Child Theme
Template: chitui-theme
*/
```
3. Create `functions.php`:
```php
<?php
add_action('wp_enqueue_scripts', 'chitui_child_enqueue_styles');
function chitui_child_enqueue_styles() {
    wp_enqueue_style('parent-style', get_template_directory_uri() . '/style.css');
}
```

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)
- Mobile browsers (iOS Safari, Chrome Mobile)

## Credits

- Inspired by ChitUI - 3D Printer Control Interface
- Built with WordPress best practices
- Uses system fonts for optimal performance
- Icon support for Bootstrap Icons (optional)

## Changelog

### Version 1.0.0
- Initial release
- Dark theme with red accents
- Responsive sidebar navigation
- Card-based layouts
- Custom widgets and template parts
- Full WordPress feature support

## Support

For issues and questions:
- GitHub: https://github.com/xmodpt/ChitUI_v1
- Documentation: See this README file

## License

This theme is licensed under the GNU General Public License v2 or later.
License URI: http://www.gnu.org/licenses/gpl-2.0.html

## Author

ChitUI Team
- GitHub: https://github.com/xmodpt

---

**Note:** This theme is inspired by the ChitUI 3D printer control interface and brings its modern, dark aesthetic to WordPress websites.
