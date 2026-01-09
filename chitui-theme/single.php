<?php
/**
 * The template for displaying single posts
 *
 * @package ChitUI_Theme
 * @since 1.0.0
 */

get_header();
?>

<main id="primary" class="site-main">
    <div class="content-wrapper">

        <?php
        while (have_posts()) :
            the_post();

            get_template_part('template-parts/content', 'single');

            // Post navigation
            the_post_navigation(array(
                'prev_text' => '<span class="nav-subtitle">' . esc_html__('Previous:', 'chitui-theme') . '</span> <span class="nav-title">%title</span>',
                'next_text' => '<span class="nav-subtitle">' . esc_html__('Next:', 'chitui-theme') . '</span> <span class="nav-title">%title</span>',
            ));

            // If comments are open or we have at least one comment, load up the comment template
            if (comments_open() || get_comments_number()) :
                comments_template();
            endif;

        endwhile;
        ?>

    </div>
</main>

<?php
get_sidebar();
get_footer();
