/**
 * Navigation Scripts
 *
 * Handles sidebar toggle and mobile menu functionality
 *
 * @package ChitUI_Theme
 * @since 1.0.0
 */

(function($) {
    'use strict';

    $(document).ready(function() {

        // Sidebar toggle functionality
        const menuToggle = $('.menu-toggle');
        const sidebar = $('.app-sidebar');
        const overlay = $('.sidebar-overlay');
        const body = $('body');

        // Toggle sidebar on menu button click
        menuToggle.on('click', function(e) {
            e.preventDefault();
            sidebar.toggleClass('active');
            overlay.toggleClass('active');
            body.toggleClass('sidebar-open');

            // Update aria-expanded attribute
            const expanded = $(this).attr('aria-expanded') === 'true';
            $(this).attr('aria-expanded', !expanded);
        });

        // Close sidebar when clicking overlay
        overlay.on('click', function() {
            sidebar.removeClass('active');
            overlay.removeClass('active');
            body.removeClass('sidebar-open');
            menuToggle.attr('aria-expanded', 'false');
        });

        // Close sidebar on ESC key
        $(document).on('keydown', function(e) {
            if (e.key === 'Escape' && sidebar.hasClass('active')) {
                sidebar.removeClass('active');
                overlay.removeClass('active');
                body.removeClass('sidebar-open');
                menuToggle.attr('aria-expanded', 'false');
            }
        });

        // Close sidebar when screen size changes to desktop
        let resizeTimer;
        $(window).on('resize', function() {
            clearTimeout(resizeTimer);
            resizeTimer = setTimeout(function() {
                if ($(window).width() > 992) {
                    sidebar.removeClass('active');
                    overlay.removeClass('active');
                    body.removeClass('sidebar-open');
                    menuToggle.attr('aria-expanded', 'false');
                }
            }, 250);
        });

        // Submenu toggle for mobile (if using nested menus)
        $('.menu-item-has-children > a').on('click', function(e) {
            if ($(window).width() <= 992) {
                e.preventDefault();
                $(this).parent().toggleClass('open');
                $(this).next('.sub-menu').slideToggle(200);
            }
        });

    });

})(jQuery);
