<?php
/**
 * Template part for displaying posts
 *
 * @package ChitUI_Theme
 * @since 1.0.0
 */
?>

<article id="post-<?php the_ID(); ?>" <?php post_class('dashboard-card'); ?>>
    <header class="entry-header">
        <?php chitui_post_badge(); ?>

        <?php
        if (is_singular()) :
            the_title('<h1 class="entry-title">', '</h1>');
        else :
            the_title('<h2 class="entry-title"><a href="' . esc_url(get_permalink()) . '">', '</a></h2>');
        endif;
        ?>

        <?php if ('post' === get_post_type()) : ?>
            <div class="entry-meta">
                <?php
                chitui_posted_on();
                chitui_posted_by();
                chitui_categories();
                chitui_comments_link();
                ?>
            </div>
        <?php endif; ?>
    </header>

    <?php if (has_post_thumbnail()) : ?>
        <div class="post-thumbnail">
            <?php
            if (is_singular()) :
                the_post_thumbnail('large');
            else :
                ?>
                <a href="<?php the_permalink(); ?>">
                    <?php the_post_thumbnail('chitui-featured'); ?>
                </a>
                <?php
            endif;
            ?>
        </div>
    <?php endif; ?>

    <div class="entry-content">
        <?php
        if (is_singular()) :
            the_content();
        else :
            the_excerpt();
        endif;

        wp_link_pages(array(
            'before' => '<div class="page-links">' . esc_html__('Pages:', 'chitui-theme'),
            'after'  => '</div>',
        ));
        ?>
    </div>

    <?php if (is_singular() && ('post' === get_post_type())) : ?>
        <footer class="entry-footer">
            <?php chitui_tags(); ?>
        </footer>
    <?php endif; ?>
</article>
