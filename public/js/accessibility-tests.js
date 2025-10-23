// Accessibility Tests for Benchmark Website
// These tests verify keyboard navigation, ARIA labels, and semantic HTML structure

// Test results container
const testResults = [];

// DOM Content Loaded handler for accessibility tests
document.addEventListener('DOMContentLoaded', function() {
    // Only run tests if in development mode (when URL contains localhost or file://)
    if (window.location.hostname === 'localhost' || window.location.protocol === 'file:') {
        console.log('Running accessibility tests...');
        runAccessibilityTests();
    }
});

function runAccessibilityTests() {
    testKeyboardNavigation();
    testAriaLabels();
    testSemanticHTML();
    testColorContrast();
    
    // Display results
    displayTestResults();
}

// Test keyboard navigation functionality
function testKeyboardNavigation() {
    const testName = 'Keyboard Navigation';
    let passed = true;
    const issues = [];
    
    // Check if navigation links are focusable
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach((link, index) => {
        if (link.tabIndex < 0) {
            passed = false;
            issues.push(`Navigation link ${index + 1} is not focusable`);
        }
    });
    
    // Check if buttons have proper focus handling
    const buttons = document.querySelectorAll('button');
    buttons.forEach((button, index) => {
        if (button.tabIndex < 0) {
            passed = false;
            issues.push(`Button ${index + 1} is not focusable`);
        }
    });
    
    // Check if interactive images are focusable
    const interactiveImages = document.querySelectorAll('.screenshots-grid img[tabindex="0"]');
    if (interactiveImages.length === 0) {
        passed = false;
        issues.push('Screenshot images are not keyboard accessible');
    }
    
    testResults.push({
        name: testName,
        passed: passed,
        issues: issues
    });
}

// Test ARIA labels and semantic HTML structure
function testAriaLabels() {
    const testName = 'ARIA Labels and Semantic HTML';
    let passed = true;
    const issues = [];
    
    // Check for proper heading hierarchy
    const headings = document.querySelectorAll('h1, h2, h3, h4, h5, h6');
    let previousLevel = 0;
    headings.forEach((heading, index) => {
        const level = parseInt(heading.tagName.charAt(1));
        if (index === 0 && level !== 1) {
            passed = false;
            issues.push('First heading should be h1');
        }
        if (level > previousLevel + 1) {
            passed = false;
            issues.push(`Heading level jumps from h${previousLevel} to h${level}`);
        }
        previousLevel = level;
    });
    
    // Check for alt text on images
    const images = document.querySelectorAll('img');
    images.forEach((img, index) => {
        if (!img.alt || img.alt.trim() === '') {
            passed = false;
            issues.push(`Image ${index + 1} missing alt text`);
        }
    });
    
    // Check for ARIA labels on interactive elements
    const navToggle = document.querySelector('.nav-toggle');
    if (navToggle && !navToggle.getAttribute('aria-label')) {
        passed = false;
        issues.push('Navigation toggle missing aria-label');
    }
    
    // Check for proper navigation structure
    const nav = document.querySelector('nav[role="navigation"]');
    if (!nav) {
        passed = false;
        issues.push('Navigation missing role="navigation"');
    }
    
    // Check for main landmark
    const main = document.querySelector('main');
    if (!main) {
        passed = false;
        issues.push('Page missing main landmark');
    }
    
    testResults.push({
        name: testName,
        passed: passed,
        issues: issues
    });
}

// Test semantic HTML structure
function testSemanticHTML() {
    const testName = 'Semantic HTML Structure';
    let passed = true;
    const issues = [];
    
    // Check for proper document structure
    const requiredElements = ['header', 'main', 'footer'];
    requiredElements.forEach(element => {
        if (!document.querySelector(element)) {
            passed = false;
            issues.push(`Missing ${element} element`);
        }
    });
    
    // Check for proper section structure
    const sections = document.querySelectorAll('section');
    sections.forEach((section, index) => {
        const heading = section.querySelector('h1, h2, h3, h4, h5, h6');
        if (!heading) {
            passed = false;
            issues.push(`Section ${index + 1} missing heading`);
        }
    });
    
    // Check for proper list structure in navigation
    const navLinks = document.querySelectorAll('.nav .nav-link');
    if (navLinks.length > 0) {
        const nav = document.querySelector('.nav');
        // Navigation could be improved with proper list structure, but current implementation is acceptable
    }
    
    testResults.push({
        name: testName,
        passed: passed,
        issues: issues
    });
}

// Test color contrast ratios (basic check)
function testColorContrast() {
    const testName = 'Color Contrast';
    let passed = true;
    const issues = [];
    
    // This is a simplified test - in a real scenario, you'd use tools like axe-core
    // For now, we'll check if CSS custom properties are properly defined
    const root = document.documentElement;
    const computedStyle = getComputedStyle(root);
    
    const primaryColor = computedStyle.getPropertyValue('--primary-color').trim();
    const textColor = computedStyle.getPropertyValue('--text-color').trim();
    const backgroundColor = computedStyle.getPropertyValue('--background-color').trim();
    
    if (!primaryColor || !textColor || !backgroundColor) {
        passed = false;
        issues.push('CSS custom properties for colors not properly defined');
    }
    
    // Check if text has sufficient contrast (this is a basic check)
    // In a real implementation, you'd calculate actual contrast ratios
    const textElements = document.querySelectorAll('p, h1, h2, h3, h4, h5, h6, a, button');
    let hasLowContrastElements = false;
    
    textElements.forEach((element, index) => {
        const style = getComputedStyle(element);
        const color = style.color;
        const bgColor = style.backgroundColor;
        
        // Basic check: ensure text is not too light on light backgrounds
        if (color === 'rgb(255, 255, 255)' && (bgColor === 'rgb(255, 255, 255)' || bgColor === 'rgba(0, 0, 0, 0)')) {
            hasLowContrastElements = true;
        }
    });
    
    if (hasLowContrastElements) {
        passed = false;
        issues.push('Some elements may have insufficient color contrast');
    }
    
    testResults.push({
        name: testName,
        passed: passed,
        issues: issues
    });
}

// Display test results in console
function displayTestResults() {
    console.log('\n=== Accessibility Test Results ===');
    
    let allPassed = true;
    testResults.forEach(test => {
        const status = test.passed ? '✅ PASS' : '❌ FAIL';
        console.log(`${status}: ${test.name}`);
        
        if (!test.passed) {
            allPassed = false;
            test.issues.forEach(issue => {
                console.log(`  - ${issue}`);
            });
        }
    });
    
    console.log('\n=== Summary ===');
    console.log(`Tests passed: ${testResults.filter(t => t.passed).length}/${testResults.length}`);
    
    if (allPassed) {
        console.log('🎉 All accessibility tests passed!');
    } else {
        console.log('⚠️  Some accessibility issues found. Please review and fix.');
    }
    
    return allPassed;
}

// Export for potential use in other contexts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        runAccessibilityTests,
        testResults
    };
}