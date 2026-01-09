/**
 * Main JavaScript
 *
 * Handles interactive features and enhancements
 *
 * @package ChitUI_Theme
 * @since 1.0.0
 */

(function($) {
    'use strict';

    $(document).ready(function() {

        // Smooth scroll to top
        $(window).scroll(function() {
            if ($(this).scrollTop() > 100) {
                $('#scroll-to-top').fadeIn();
            } else {
                $('#scroll-to-top').fadeOut();
            }
        });

        $('#scroll-to-top').on('click', function(e) {
            e.preventDefault();
            $('html, body').animate({ scrollTop: 0 }, 600);
        });

        // Smooth scrolling for anchor links
        $('a[href*="#"]:not([href="#"])').on('click', function(e) {
            if (location.pathname.replace(/^\//, '') === this.pathname.replace(/^\//, '') &&
                location.hostname === this.hostname) {

                let target = $(this.hash);
                target = target.length ? target : $('[name=' + this.hash.slice(1) + ']');

                if (target.length) {
                    e.preventDefault();
                    $('html, body').animate({
                        scrollTop: target.offset().top - 80
                    }, 600);
                }
            }
        });

        // Add class to cards on hover for enhanced effects
        $('.dashboard-card').hover(
            function() {
                $(this).addClass('hovered');
            },
            function() {
                $(this).removeClass('hovered');
            }
        );

        // External links open in new tab
        $('a[href^="http"]').not('[href*="' + window.location.hostname + '"]').attr({
            target: '_blank',
            rel: 'noopener noreferrer'
        });

        // Responsive tables
        $('table').wrap('<div class="table-responsive"></div>');

        // Add class to images for lazy loading effect
        $('img').not('.no-lazy').each(function() {
            if (!$(this).hasClass('lazy-loaded')) {
                $(this).on('load', function() {
                    $(this).addClass('lazy-loaded');
                });
            }
        });

        // Sidebar pin functionality (optional)
        const pinButton = $('#sidebar-pin-button');
        const sidebar = $('.app-sidebar');
        const content = $('.app-content');
        const footer = $('.site-footer');

        if (pinButton.length) {
            pinButton.on('click', function(e) {
                e.preventDefault();

                sidebar.toggleClass('pinned');
                content.toggleClass('sidebar-pinned');
                footer.toggleClass('sidebar-pinned');

                // Save preference to localStorage
                if (sidebar.hasClass('pinned')) {
                    localStorage.setItem('chitui_sidebar_pinned', 'true');
                    $(this).html('<i class="icon-unpin"></i>');
                } else {
                    localStorage.setItem('chitui_sidebar_pinned', 'false');
                    $(this).html('<i class="icon-pin"></i>');
                }
            });

            // Load saved preference
            if (localStorage.getItem('chitui_sidebar_pinned') === 'true' && $(window).width() > 992) {
                sidebar.addClass('pinned');
                content.addClass('sidebar-pinned');
                footer.addClass('sidebar-pinned');
                pinButton.html('<i class="icon-unpin"></i>');
            }
        }

        // Comment form enhancements
        if ($('#commentform').length) {
            $('#commentform input[type="submit"]').addClass('btn btn-accent');
        }

        // Search form enhancements
        $('.search-form input[type="search"]').attr('placeholder', 'Search...');

        // Add animation class when element is in viewport
        function isInViewport(element) {
            const rect = element.getBoundingClientRect();
            return (
                rect.top >= 0 &&
                rect.left >= 0 &&
                rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
                rect.right <= (window.innerWidth || document.documentElement.clientWidth)
            );
        }

        function checkAnimation() {
            $('.dashboard-card:not(.animated)').each(function() {
                if (isInViewport(this)) {
                    $(this).addClass('animated fadeIn');
                }
            });
        }

        $(window).on('scroll resize', checkAnimation);
        checkAnimation(); // Check on load

        // Print functionality
        $('.print-button').on('click', function(e) {
            e.preventDefault();
            window.print();
        });

        // Share buttons (if implemented)
        $('.share-button').on('click', function(e) {
            e.preventDefault();
            const url = encodeURIComponent(window.location.href);
            const title = encodeURIComponent(document.title);
            const platform = $(this).data('platform');

            let shareUrl = '';

            switch(platform) {
                case 'twitter':
                    shareUrl = `https://twitter.com/intent/tweet?url=${url}&text=${title}`;
                    break;
                case 'facebook':
                    shareUrl = `https://www.facebook.com/sharer/sharer.php?u=${url}`;
                    break;
                case 'linkedin':
                    shareUrl = `https://www.linkedin.com/sharing/share-offsite/?url=${url}`;
                    break;
            }

            if (shareUrl) {
                window.open(shareUrl, 'share', 'width=600,height=400');
            }
        });

    });

})(jQuery);
