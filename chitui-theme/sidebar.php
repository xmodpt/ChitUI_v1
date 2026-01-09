<?php
/**
 * The sidebar template file
 *
 * @package ChitUI_Theme
 * @since 1.0.0
 */

if (!is_active_sidebar('sidebar-1')) {
    return;
}
?>

<aside class="app-sidebar" id="secondary">
    <div class="sidebar-content">

        <?php if (has_nav_menu('primary')) : ?>
            <div class="sidebar-section">
                <h4 class="sidebar-section-title"><?php esc_html_e('Navigation', 'chitui-theme'); ?></h4>
                <?php
                wp_nav_menu(array(
                    'theme_location' => 'primary',
                    'menu_class'     => 'sidebar-menu',
                    'container'      => 'nav',
                    'fallback_cb'    => false,
                ));
                ?>
            </div>
        <?php endif; ?>

        <?php if (is_active_sidebar('sidebar-1')) : ?>
            <div class="sidebar-section">
                <h4 class="sidebar-section-title"><?php esc_html_e('Widgets', 'chitui-theme'); ?></h4>
                <?php dynamic_sidebar('sidebar-1'); ?>
            </div>
        <?php endif; ?>

    </div>
</aside>
