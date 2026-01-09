<?php
/**
 * The template for displaying archive pages
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
                <?php
                the_archive_title('<h1 class="page-title">', '</h1>');
                the_archive_description('<div class="archive-description">', '</div>');
                ?>
            </header>

            <div class="posts-grid">
                <?php
                // Start the Loop
                while (have_posts()) :
                    the_post();
                    get_template_part('template-parts/content', get_post_format());
                endwhile;
                ?>
            </div>

            <?php
            // Pagination
            chitui_pagination();

        else :
            // No posts found
            get_template_part('template-parts/content', 'none');

        endif;
        ?>

    </div>
</main>

<?php
get_sidebar();
get_footer();
