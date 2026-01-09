<?php
/**
 * The template for displaying comments
 *
 * @package ChitUI_Theme
 * @since 1.0.0
 */

if (post_password_required()) {
    return;
}
?>

<div id="comments" class="comments-area dashboard-card">

    <?php if (have_comments()) : ?>
        <h2 class="comments-title">
            <?php
            $comment_count = get_comments_number();
            if ('1' === $comment_count) {
                printf(
                    esc_html__('One comment on &ldquo;%s&rdquo;', 'chitui-theme'),
                    '<span>' . get_the_title() . '</span>'
                );
            } else {
                printf(
                    esc_html(_nx(
                        '%1$s comment on &ldquo;%2$s&rdquo;',
                        '%1$s comments on &ldquo;%2$s&rdquo;',
                        $comment_count,
                        'comments title',
                        'chitui-theme'
                    )),
                    number_format_i18n($comment_count),
                    '<span>' . get_the_title() . '</span>'
                );
            }
            ?>
        </h2>

        <ol class="comment-list">
            <?php
            wp_list_comments(array(
                'style'       => 'ol',
                'short_ping'  => true,
                'avatar_size' => 50,
                'callback'    => 'chitui_comment_callback',
            ));
            ?>
        </ol>

        <?php
        the_comments_navigation(array(
            'prev_text' => '<span class="nav-subtitle">' . esc_html__('Older Comments', 'chitui-theme') . '</span>',
            'next_text' => '<span class="nav-subtitle">' . esc_html__('Newer Comments', 'chitui-theme') . '</span>',
        ));

        if (!comments_open()) :
            ?>
            <p class="no-comments"><?php esc_html_e('Comments are closed.', 'chitui-theme'); ?></p>
        <?php
        endif;

    endif;

    comment_form(array(
        'title_reply_before' => '<h3 id="reply-title" class="comment-reply-title">',
        'title_reply_after'  => '</h3>',
        'class_submit'       => 'btn btn-accent',
    ));
    ?>

</div>

<?php
/**
 * Custom comment callback
 */
function chitui_comment_callback($comment, $args, $depth) {
    ?>
    <li <?php comment_class('comment'); ?> id="comment-<?php comment_ID(); ?>">
        <article id="div-comment-<?php comment_ID(); ?>" class="comment-body">
            <footer class="comment-meta">
                <div class="comment-author vcard">
                    <?php echo get_avatar($comment, $args['avatar_size']); ?>
                    <b class="fn"><?php echo get_comment_author_link($comment); ?></b>
                    <span class="says"><?php esc_html_e('says:', 'chitui-theme'); ?></span>
                </div>

                <div class="comment-metadata">
                    <a href="<?php echo esc_url(get_comment_link($comment, $args)); ?>">
                        <time datetime="<?php comment_time('c'); ?>">
                            <?php
                            printf(
                                esc_html__('%1$s at %2$s', 'chitui-theme'),
                                get_comment_date('', $comment),
                                get_comment_time()
                            );
                            ?>
                        </time>
                    </a>
                    <?php edit_comment_link(esc_html__('Edit', 'chitui-theme'), '<span class="edit-link">', '</span>'); ?>
                </div>

                <?php if ('0' == $comment->comment_approved) : ?>
                    <p class="comment-awaiting-moderation badge badge-warning">
                        <?php esc_html_e('Your comment is awaiting moderation.', 'chitui-theme'); ?>
                    </p>
                <?php endif; ?>
            </footer>

            <div class="comment-content">
                <?php comment_text(); ?>
            </div>

            <?php
            comment_reply_link(array_merge($args, array(
                'add_below' => 'div-comment',
                'depth'     => $depth,
                'max_depth' => $args['max_depth'],
                'before'    => '<div class="reply">',
                'after'     => '</div>',
            )));
            ?>
        </article>
    <?php
}
