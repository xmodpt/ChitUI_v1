<?php
/**
 * The header template file
 *
 * @package ChitUI_Theme
 * @since 1.0.0
 */
?>
<!DOCTYPE html>
<html <?php language_attributes(); ?>>
<head>
    <meta charset="<?php bloginfo('charset'); ?>">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <link rel="profile" href="https://gmpg.org/xfn/11">
    <?php wp_head(); ?>
</head>

<body <?php body_class(); ?>>
<?php wp_body_open(); ?>

<div class="site-wrapper">

    <!-- Header -->
    <header class="app-header">
        <div class="site-branding">
            <?php chitui_custom_logo(); ?>
            <div class="site-info">
                <?php if (is_front_page() && is_home()) : ?>
                    <h1 class="site-title"><a href="<?php echo esc_url(home_url('/')); ?>"><?php bloginfo('name'); ?></a></h1>
                <?php else : ?>
                    <p class="site-title"><a href="<?php echo esc_url(home_url('/')); ?>"><?php bloginfo('name'); ?></a></p>
                <?php endif; ?>

                <?php
                $description = get_bloginfo('description', 'display');
                if ($description || is_customize_preview()) :
                    ?>
                    <p class="site-description"><?php echo $description; ?></p>
                <?php endif; ?>
            </div>
        </div>

        <div class="header-actions">
            <?php if (has_nav_menu('primary')) : ?>
                <button class="menu-toggle" aria-controls="primary-menu" aria-expanded="false">
                    <span class="menu-icon">☰</span>
                </button>
            <?php endif; ?>
        </div>
    </header>

    <!-- Sidebar Overlay for Mobile -->
    <div class="sidebar-overlay"></div>

    <!-- Sidebar -->
    <?php get_sidebar(); ?>

    <!-- Main Content Area -->
    <div class="app-content">
