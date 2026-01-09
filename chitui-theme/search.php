<?php
/**
 * The template for displaying search results
 *
 * @package ChitUI_Theme
 * @since 1.0.0
 */

get_header();
?>

<main id="primary" class="site-main">
    <div class="content-wrapper">

        <?php if (have_posts()) : ?>

            <header class="page-header dashboard-card">
                <h1 class="page-title">
                    <?php
                    printf(
                        esc_html__('Search Results for: %s', 'chitui-theme'),
                        '<span class="badge badge-accent">' . get_search_query() . '</span>'
                    );
                    ?>
                </h1>
            </header>

            <div class="posts-grid">
                <?php
                while (have_posts()) :
                    the_post();
                    get_template_part('template-parts/content', get_post_format());
                endwhile;
                ?>
            </div>

            <?php chitui_pagination(); ?>

        <?php else : ?>

            <?php get_template_part('template-parts/content', 'none'); ?>

        <?php endif; ?>

    </div>
</main>

<?php
get_sidebar();
get_footer();
