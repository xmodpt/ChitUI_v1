<?php
/**
 * The template for displaying 404 pages (not found)
 *
 * @package ChitUI_Theme
 * @since 1.0.0
 */

get_header();
?>

<main id="primary" class="site-main">
    <div class="content-wrapper">

        <article class="dashboard-card error-404 not-found">
            <header class="page-header">
                <h1 class="page-title"><?php esc_html_e('404 - Page Not Found', 'chitui-theme'); ?></h1>
            </header>

            <div class="page-content">
                <p><?php esc_html_e('It looks like nothing was found at this location. Maybe try searching?', 'chitui-theme'); ?></p>

                <?php get_search_form(); ?>

                <div class="error-404-widgets" style="margin-top: 2rem;">
                    <h3><?php esc_html_e('Try looking in the monthly archives', 'chitui-theme'); ?></h3>
                    <?php
                    wp_get_archives(array(
                        'type'  => 'monthly',
                        'limit' => 12,
                    ));
                    ?>
                </div>
            </div>
        </article>

    </div>
</main>

<?php
get_footer();
