<?php
/**
 * The footer template file
 *
 * @package ChitUI_Theme
 * @since 1.0.0
 */
?>

    </div><!-- .app-content -->

    <footer class="site-footer">
        <?php if (is_active_sidebar('footer-1') || is_active_sidebar('footer-2') || is_active_sidebar('footer-3')) : ?>
            <div class="footer-content">
                <?php if (is_active_sidebar('footer-1')) : ?>
                    <div class="footer-column">
                        <?php dynamic_sidebar('footer-1'); ?>
                    </div>
                <?php endif; ?>

                <?php if (is_active_sidebar('footer-2')) : ?>
                    <div class="footer-column">
                        <?php dynamic_sidebar('footer-2'); ?>
                    </div>
                <?php endif; ?>

                <?php if (is_active_sidebar('footer-3')) : ?>
                    <div class="footer-column">
                        <?php dynamic_sidebar('footer-3'); ?>
                    </div>
                <?php endif; ?>
            </div>
        <?php endif; ?>

        <div class="footer-bottom">
            <p>
                &copy; <?php echo date('Y'); ?>
                <a href="<?php echo esc_url(home_url('/')); ?>"><?php bloginfo('name'); ?></a>
                <?php if (has_nav_menu('footer')) : ?>
                    <span class="sep"> | </span>
                    <?php
                    wp_nav_menu(array(
                        'theme_location' => 'footer',
                        'menu_class'     => 'footer-menu',
                        'container'      => 'nav',
                        'depth'          => 1,
                    ));
                    ?>
                <?php endif; ?>
            </p>
            <p>
                <?php
                /* translators: %s: Theme name */
                printf(esc_html__('Theme: %s', 'chitui-theme'), 'ChitUI Theme');
                ?>
            </p>
        </div>
    </footer>

</div><!-- .site-wrapper -->

<?php wp_footer(); ?>

</body>
</html>
