<?php
/**
 * ChitUI Theme Functions
 *
 * @package ChitUI_Theme
 * @since 1.0.0
 */

if (!defined('ABSPATH')) {
    exit; // Exit if accessed directly
}

/**
 * Theme Setup
 */
function chitui_theme_setup() {
    // Add default posts and comments RSS feed links to head
    add_theme_support('automatic-feed-links');

    // Let WordPress manage the document title
    add_theme_support('title-tag');

    // Enable support for Post Thumbnails on posts and pages
    add_theme_support('post-thumbnails');
    set_post_thumbnail_size(1200, 675, true);

    // Add custom image sizes
    add_image_size('chitui-featured', 800, 450, true);
    add_image_size('chitui-thumbnail', 400, 300, true);

    // Register navigation menus
    register_nav_menus(array(
        'primary' => __('Primary Menu', 'chitui-theme'),
        'footer'  => __('Footer Menu', 'chitui-theme'),
    ));

    // Switch default core markup to output valid HTML5
    add_theme_support('html5', array(
        'search-form',
        'comment-form',
        'comment-list',
        'gallery',
        'caption',
        'style',
        'script',
    ));

    // Add theme support for selective refresh for widgets
    add_theme_support('customize-selective-refresh-widgets');

    // Add support for custom logo
    add_theme_support('custom-logo', array(
        'height'      => 100,
        'width'       => 400,
        'flex-height' => true,
        'flex-width'  => true,
    ));

    // Add support for custom background
    add_theme_support('custom-background', array(
        'default-color' => '1a1a1a',
    ));

    // Add support for editor styles
    add_theme_support('editor-styles');
    add_editor_style('css/editor-style.css');

    // Add support for responsive embeds
    add_theme_support('responsive-embeds');

    // Add support for wide and full alignment
    add_theme_support('align-wide');
}
add_action('after_setup_theme', 'chitui_theme_setup');

/**
 * Set the content width in pixels
 */
function chitui_content_width() {
    $GLOBALS['content_width'] = apply_filters('chitui_content_width', 1200);
}
add_action('after_setup_theme', 'chitui_content_width', 0);

/**
 * Register Widget Areas
 */
function chitui_widgets_init() {
    register_sidebar(array(
        'name'          => __('Primary Sidebar', 'chitui-theme'),
        'id'            => 'sidebar-1',
        'description'   => __('Add widgets here to appear in your sidebar.', 'chitui-theme'),
        'before_widget' => '<div id="%1$s" class="widget %2$s">',
        'after_widget'  => '</div>',
        'before_title'  => '<h3 class="widget-title">',
        'after_title'   => '</h3>',
    ));

    register_sidebar(array(
        'name'          => __('Footer Widget Area 1', 'chitui-theme'),
        'id'            => 'footer-1',
        'description'   => __('Add widgets here to appear in your footer.', 'chitui-theme'),
        'before_widget' => '<div id="%1$s" class="footer-widget %2$s">',
        'after_widget'  => '</div>',
        'before_title'  => '<h4 class="footer-widget-title">',
        'after_title'   => '</h4>',
    ));

    register_sidebar(array(
        'name'          => __('Footer Widget Area 2', 'chitui-theme'),
        'id'            => 'footer-2',
        'description'   => __('Add widgets here to appear in your footer.', 'chitui-theme'),
        'before_widget' => '<div id="%1$s" class="footer-widget %2$s">',
        'after_widget'  => '</div>',
        'before_title'  => '<h4 class="footer-widget-title">',
        'after_title'   => '</h4>',
    ));

    register_sidebar(array(
        'name'          => __('Footer Widget Area 3', 'chitui-theme'),
        'id'            => 'footer-3',
        'description'   => __('Add widgets here to appear in your footer.', 'chitui-theme'),
        'before_widget' => '<div id="%1$s" class="footer-widget %2$s">',
        'after_widget'  => '</div>',
        'before_title'  => '<h4 class="footer-widget-title">',
        'after_title'   => '</h4>',
    ));
}
add_action('widgets_init', 'chitui_widgets_init');

/**
 * Enqueue Scripts and Styles
 */
function chitui_scripts() {
    // Main stylesheet
    wp_enqueue_style('chitui-style', get_stylesheet_uri(), array(), '1.0.0');

    // Custom CSS
    wp_enqueue_style('chitui-custom', get_template_directory_uri() . '/css/custom.css', array(), '1.0.0');

    // Main JavaScript
    wp_enqueue_script('chitui-navigation', get_template_directory_uri() . '/js/navigation.js', array('jquery'), '1.0.0', true);

    // Smooth scroll and interactive features
    wp_enqueue_script('chitui-main', get_template_directory_uri() . '/js/main.js', array('jquery'), '1.0.0', true);

    // Threaded comments
    if (is_singular() && comments_open() && get_option('thread_comments')) {
        wp_enqueue_script('comment-reply');
    }

    // Localize script for AJAX
    wp_localize_script('chitui-main', 'chitui_ajax', array(
        'ajax_url' => admin_url('admin-ajax.php'),
        'nonce'    => wp_create_nonce('chitui-nonce'),
    ));
}
add_action('wp_enqueue_scripts', 'chitui_scripts');

/**
 * Custom Excerpt Length
 */
function chitui_excerpt_length($length) {
    return 40;
}
add_filter('excerpt_length', 'chitui_excerpt_length', 999);

/**
 * Custom Excerpt More
 */
function chitui_excerpt_more($more) {
    return '... <a href="' . get_permalink() . '" class="read-more">' . __('Read More', 'chitui-theme') . '</a>';
}
add_filter('excerpt_more', 'chitui_excerpt_more');

/**
 * Add custom classes to body
 */
function chitui_body_classes($classes) {
    // Add a class if sidebar is active
    if (is_active_sidebar('sidebar-1')) {
        $classes[] = 'has-sidebar';
    }

    // Add a class for single posts
    if (is_singular()) {
        $classes[] = 'singular';
    }

    return $classes;
}
add_filter('body_class', 'chitui_body_classes');

/**
 * Custom post meta display
 */
function chitui_posted_on() {
    $time_string = '<time class="entry-date published" datetime="%1$s">%2$s</time>';

    $time_string = sprintf($time_string,
        esc_attr(get_the_date(DATE_W3C)),
        esc_html(get_the_date())
    );

    echo '<span class="posted-on"><i class="icon-calendar"></i> ' . $time_string . '</span>';
}

/**
 * Custom author meta display
 */
function chitui_posted_by() {
    echo '<span class="byline"><i class="icon-user"></i> <a href="' . esc_url(get_author_posts_url(get_the_author_meta('ID'))) . '">' . esc_html(get_the_author()) . '</a></span>';
}

/**
 * Custom category display
 */
function chitui_categories() {
    $categories_list = get_the_category_list(', ');
    if ($categories_list) {
        echo '<span class="cat-links"><i class="icon-folder"></i> ' . $categories_list . '</span>';
    }
}

/**
 * Custom tags display
 */
function chitui_tags() {
    $tags_list = get_the_tag_list('', ' ');
    if ($tags_list) {
        echo '<div class="post-tags">' . $tags_list . '</div>';
    }
}

/**
 * Custom comment count display
 */
function chitui_comments_link() {
    if (!is_single() && !post_password_required() && (comments_open() || get_comments_number())) {
        echo '<span class="comments-link"><i class="icon-comment"></i> ';
        comments_popup_link(
            __('Leave a comment', 'chitui-theme'),
            __('1 Comment', 'chitui-theme'),
            __('% Comments', 'chitui-theme')
        );
        echo '</span>';
    }
}

/**
 * Add status badge based on post format or custom field
 */
function chitui_post_badge() {
    $post_format = get_post_format();

    if ($post_format === 'video') {
        echo '<span class="badge badge-info">Video</span>';
    } elseif ($post_format === 'gallery') {
        echo '<span class="badge badge-warning">Gallery</span>';
    } elseif (is_sticky()) {
        echo '<span class="badge badge-accent">Featured</span>';
    }
}

/**
 * Pagination
 */
function chitui_pagination() {
    global $wp_query;

    if ($wp_query->max_num_pages <= 1) {
        return;
    }

    $paged = get_query_var('paged') ? absint(get_query_var('paged')) : 1;
    $max   = intval($wp_query->max_num_pages);

    // Add current page to the array
    if ($paged >= 1) {
        $links[] = $paged;
    }

    // Add the pages around the current page
    if ($paged >= 3) {
        $links[] = $paged - 1;
        $links[] = $paged - 2;
    }

    if (($paged + 2) <= $max) {
        $links[] = $paged + 2;
        $links[] = $paged + 1;
    }

    echo '<nav class="pagination" role="navigation">';

    // Previous link
    if (get_previous_posts_link()) {
        printf('<a href="%s" class="page-numbers prev">%s</a>', get_previous_posts_page_link(), __('&laquo; Previous', 'chitui-theme'));
    }

    // Link to first page
    if (!in_array(1, $links)) {
        $class = 1 == $paged ? ' current' : '';
        printf('<a href="%s" class="page-numbers%s">%s</a>', esc_url(get_pagenum_link(1)), $class, '1');

        if (!in_array(2, $links)) {
            echo '<span class="page-numbers dots">...</span>';
        }
    }

    // Link to current page, plus 2 pages in either direction
    sort($links);
    foreach ((array) $links as $link) {
        $class = $paged == $link ? ' current' : '';
        printf('<a href="%s" class="page-numbers%s">%s</a>', esc_url(get_pagenum_link($link)), $class, $link);
    }

    // Link to last page
    if (!in_array($max, $links)) {
        if (!in_array($max - 1, $links)) {
            echo '<span class="page-numbers dots">...</span>';
        }

        $class = $paged == $max ? ' current' : '';
        printf('<a href="%s" class="page-numbers%s">%s</a>', esc_url(get_pagenum_link($max)), $class, $max);
    }

    // Next link
    if (get_next_posts_link()) {
        printf('<a href="%s" class="page-numbers next">%s</a>', get_next_posts_page_link(), __('Next &raquo;', 'chitui-theme'));
    }

    echo '</nav>';
}

/**
 * Custom logo display
 */
function chitui_custom_logo() {
    if (has_custom_logo()) {
        the_custom_logo();
    } else {
        echo '<div class="site-logo">';
        echo '<svg width="40" height="40" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">';
        echo '<rect width="40" height="40" rx="8" fill="#ff4757"/>';
        echo '<path d="M20 10C14.477 10 10 14.477 10 20C10 25.523 14.477 30 20 30C25.523 30 30 25.523 30 20C30 14.477 25.523 10 20 10ZM20 26C16.686 26 14 23.314 14 20C14 16.686 16.686 14 20 14C23.314 14 26 16.686 26 20C26 23.314 23.314 26 20 26Z" fill="white"/>';
        echo '</svg>';
        echo '</div>';
    }
}

/**
 * Breadcrumbs
 */
function chitui_breadcrumbs() {
    if (is_front_page()) {
        return;
    }

    echo '<nav class="breadcrumbs">';
    echo '<a href="' . home_url('/') . '">' . __('Home', 'chitui-theme') . '</a>';
    echo ' <span class="separator">/</span> ';

    if (is_category() || is_single()) {
        the_category(' <span class="separator">/</span> ');
        if (is_single()) {
            echo ' <span class="separator">/</span> ';
            the_title();
        }
    } elseif (is_page()) {
        echo the_title();
    } elseif (is_search()) {
        echo __('Search Results for: ', 'chitui-theme') . get_search_query();
    } elseif (is_404()) {
        echo __('404 Not Found', 'chitui-theme');
    }

    echo '</nav>';
}

/**
 * Custom Walker for Navigation Menu (optional)
 */
class ChitUI_Walker_Nav_Menu extends Walker_Nav_Menu {
    // Custom walker implementation can be added here if needed
}

/**
 * Customizer Settings
 */
function chitui_customize_register($wp_customize) {
    // Add theme color settings
    $wp_customize->add_section('chitui_colors', array(
        'title'    => __('ChitUI Colors', 'chitui-theme'),
        'priority' => 30,
    ));

    // Accent Color
    $wp_customize->add_setting('chitui_accent_color', array(
        'default'           => '#ff4757',
        'sanitize_callback' => 'sanitize_hex_color',
    ));

    $wp_customize->add_control(new WP_Customize_Color_Control($wp_customize, 'chitui_accent_color', array(
        'label'    => __('Accent Color', 'chitui-theme'),
        'section'  => 'chitui_colors',
        'settings' => 'chitui_accent_color',
    )));

    // Success Color
    $wp_customize->add_setting('chitui_success_color', array(
        'default'           => '#06d6a0',
        'sanitize_callback' => 'sanitize_hex_color',
    ));

    $wp_customize->add_control(new WP_Customize_Color_Control($wp_customize, 'chitui_success_color', array(
        'label'    => __('Success Color', 'chitui-theme'),
        'section'  => 'chitui_colors',
        'settings' => 'chitui_success_color',
    )));
}
add_action('customize_register', 'chitui_customize_register');

/**
 * Output customizer CSS
 */
function chitui_customizer_css() {
    $accent_color = get_theme_mod('chitui_accent_color', '#ff4757');
    $success_color = get_theme_mod('chitui_success_color', '#06d6a0');
    ?>
    <style type="text/css">
        :root {
            --accent-color: <?php echo esc_attr($accent_color); ?>;
            --success-color: <?php echo esc_attr($success_color); ?>;
        }
    </style>
    <?php
}
add_action('wp_head', 'chitui_customizer_css');
