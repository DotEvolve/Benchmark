// Benchmark Website JavaScript

// DOM Content Loaded handler
document.addEventListener('DOMContentLoaded', function() {
    // Initialize all interactive functionality
    initSmoothScrolling();
    initMobileNavigation();
    initImageGallery();
    
    // Log successful load only in development
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
        console.log('Benchmark website loaded');
    }
});

// Smooth scrolling navigation
function initSmoothScrolling() {
    // Get all navigation links that point to sections on the same page
    const navLinks = document.querySelectorAll('a[href^="#"]');
    
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            const href = this.getAttribute('href');
            
            // Skip if it's just a hash without target
            if (href === '#' || href === '#!') {
                return;
            }
            
            const targetId = href.substring(1);
            const targetElement = document.getElementById(targetId);
            
            if (targetElement) {
                e.preventDefault();
                
                // Calculate offset for sticky header
                const headerHeight = document.querySelector('.header').offsetHeight;
                const targetPosition = targetElement.offsetTop - headerHeight - 20;
                
                // Smooth scroll to target
                window.scrollTo({
                    top: targetPosition,
                    behavior: 'smooth'
                });
                
                // Close mobile menu if open
                const nav = document.querySelector('.nav');
                const navToggle = document.querySelector('.nav-toggle');
                if (nav.classList.contains('nav-open')) {
                    nav.classList.remove('nav-open');
                    navToggle.setAttribute('aria-expanded', 'false');
                }
            }
        });
    });
}

// Mobile navigation menu
function initMobileNavigation() {
    const navToggle = document.querySelector('.nav-toggle');
    const nav = document.querySelector('.nav');
    
    if (navToggle && nav) {
        navToggle.addEventListener('click', function() {
            const isOpen = nav.classList.contains('nav-open');
            
            if (isOpen) {
                nav.classList.remove('nav-open');
                navToggle.setAttribute('aria-expanded', 'false');
            } else {
                nav.classList.add('nav-open');
                navToggle.setAttribute('aria-expanded', 'true');
            }
        });
        
        // Close menu when clicking outside
        document.addEventListener('click', function(e) {
            if (!nav.contains(e.target) && !navToggle.contains(e.target)) {
                nav.classList.remove('nav-open');
                navToggle.setAttribute('aria-expanded', 'false');
            }
        });
        
        // Close menu on escape key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && nav.classList.contains('nav-open')) {
                nav.classList.remove('nav-open');
                navToggle.setAttribute('aria-expanded', 'false');
                navToggle.focus();
            }
        });
    }
}

// Image gallery interactions
function initImageGallery() {
    const screenshots = document.querySelectorAll('.screenshots-grid .screenshot img');
    
    screenshots.forEach((img) => {
        // Add click handler for image expansion
        img.addEventListener('click', function() {
            openImageModal(this);
        });
        
        // Add keyboard support
        img.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                openImageModal(this);
            }
        });
        
        // Make images focusable for keyboard navigation
        img.setAttribute('tabindex', '0');
        img.setAttribute('role', 'button');
        img.setAttribute('aria-label', `View larger version of ${img.alt}`);
    });
}

// Image modal functionality
function openImageModal(img) {
    // Create modal overlay
    const modal = document.createElement('div');
    modal.className = 'image-modal';
    modal.setAttribute('role', 'dialog');
    modal.setAttribute('aria-modal', 'true');
    modal.setAttribute('aria-label', 'Image gallery');
    
    // Create modal content
    const modalContent = document.createElement('div');
    modalContent.className = 'modal-content';
    
    // Create close button
    const closeBtn = document.createElement('button');
    closeBtn.className = 'modal-close';
    closeBtn.innerHTML = '×';
    closeBtn.setAttribute('aria-label', 'Close image gallery');
    
    // Create image container
    const imgContainer = document.createElement('div');
    imgContainer.className = 'modal-image-container';
    
    // Create enlarged image
    const modalImg = document.createElement('img');
    modalImg.src = img.src;
    modalImg.alt = img.alt;
    modalImg.className = 'modal-image';
    
    // Create image caption
    const caption = document.createElement('div');
    caption.className = 'modal-caption';
    caption.textContent = img.alt;
    
    // Assemble modal
    imgContainer.appendChild(modalImg);
    modalContent.appendChild(closeBtn);
    modalContent.appendChild(imgContainer);
    modalContent.appendChild(caption);
    modal.appendChild(modalContent);
    
    // Add to document
    document.body.appendChild(modal);
    document.body.style.overflow = 'hidden';
    
    // Focus management
    closeBtn.focus();
    
    // Close handlers
    function closeModal() {
        document.body.removeChild(modal);
        document.body.style.overflow = '';
        img.focus(); // Return focus to original image
    }
    
    closeBtn.addEventListener('click', closeModal);
    
    modal.addEventListener('click', function(e) {
        if (e.target === modal) {
            closeModal();
        }
    });
    
    document.addEventListener('keydown', function escapeHandler(e) {
        if (e.key === 'Escape') {
            closeModal();
            document.removeEventListener('keydown', escapeHandler);
        }
    });
    
    // Animate modal in
    requestAnimationFrame(() => {
        modal.classList.add('modal-open');
    });
}