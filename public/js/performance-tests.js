// Performance Optimization Tests for Benchmark Website
// Tests image loading performance, CSS/JS optimization, and Core Web Vitals

// Performance test results
const performanceResults = [];

// Initialize performance testing
document.addEventListener('DOMContentLoaded', function() {
    // Only run performance tests in development or when explicitly requested
    if (shouldRunPerformanceTests()) {
        console.log('Running performance optimization tests...');
        runPerformanceTests();
    }
});

// Check if performance tests should run
function shouldRunPerformanceTests() {
    return (
        window.location.hostname === 'localhost' || 
        window.location.hostname === '127.0.0.1' ||
        window.location.search.includes('perf-test=true')
    );
}

// Main performance test runner
function runPerformanceTests() {
    // Wait for page to fully load
    window.addEventListener('load', function() {
        setTimeout(() => {
            testImageLoadingPerformance();
            testResourceOptimization();
            testCoreWebVitals();
            displayPerformanceResults();
        }, 1000);
    });
}

// Test image loading performance and optimization
function testImageLoadingPerformance() {
    const testName = 'Image Loading Performance';
    let passed = true;
    const issues = [];
    const metrics = {};
    
    // Get all images on the page
    const images = document.querySelectorAll('img');
    let totalImageSize = 0;
    let imagesWithLazyLoading = 0;
    let imagesWithAltText = 0;
    
    images.forEach((img, index) => {
        // Check for lazy loading
        if (img.hasAttribute('loading') && img.getAttribute('loading') === 'lazy') {
            imagesWithLazyLoading++;
        }
        
        // Check for alt text
        if (img.alt && img.alt.trim() !== '') {
            imagesWithAltText++;
        }
        
        // Estimate image size (this is approximate)
        if (img.complete && img.naturalWidth > 0) {
            // Rough estimation based on dimensions
            const estimatedSize = (img.naturalWidth * img.naturalHeight * 3) / 1024; // KB
            totalImageSize += estimatedSize;
            
            // Check if image is too large
            if (estimatedSize > 500) { // 500KB threshold
                issues.push(`Image ${index + 1} (${img.src}) may be too large (estimated ${Math.round(estimatedSize)}KB)`);
            }
        }
    });
    
    // Performance checks
    if (images.length > 2 && imagesWithLazyLoading === 0) {
        passed = false;
        issues.push('No images use lazy loading - consider adding loading="lazy" to non-critical images');
    }
    
    if (imagesWithAltText < images.length) {
        passed = false;
        issues.push(`${images.length - imagesWithAltText} images missing alt text`);
    }
    
    if (totalImageSize > 2000) { // 2MB total threshold
        passed = false;
        issues.push(`Total estimated image size is high (${Math.round(totalImageSize)}KB)`);
    }
    
    metrics.totalImages = images.length;
    metrics.imagesWithLazyLoading = imagesWithLazyLoading;
    metrics.imagesWithAltText = imagesWithAltText;
    metrics.estimatedTotalSize = Math.round(totalImageSize);
    
    performanceResults.push({
        name: testName,
        passed: passed,
        issues: issues,
        metrics: metrics
    });
}

// Test CSS and JavaScript optimization
function testResourceOptimization() {
    const testName = 'Resource Optimization';
    let passed = true;
    const issues = [];
    const metrics = {};
    
    // Check CSS files
    const cssLinks = document.querySelectorAll('link[rel="stylesheet"]');
    let externalCssCount = 0;
    let inlineCssCount = 0;
    
    cssLinks.forEach(link => {
        if (link.href.startsWith('http')) {
            externalCssCount++;
        }
    });
    
    // Check for inline styles
    const inlineStyles = document.querySelectorAll('[style]');
    inlineCssCount = inlineStyles.length;
    
    // Check JavaScript files
    const scriptTags = document.querySelectorAll('script[src]');
    let externalJsCount = 0;
    
    scriptTags.forEach(script => {
        if (script.src.startsWith('http')) {
            externalJsCount++;
        }
    });
    
    // Check for preconnect/dns-prefetch
    const preconnectLinks = document.querySelectorAll('link[rel="preconnect"], link[rel="dns-prefetch"]');
    const hasPreconnect = preconnectLinks.length > 0;
    
    // Performance recommendations
    if (externalCssCount > 3) {
        issues.push(`Consider combining CSS files (found ${externalCssCount} external stylesheets)`);
    }
    
    if (externalJsCount > 3) {
        issues.push(`Consider combining JavaScript files (found ${externalJsCount} external scripts)`);
    }
    
    if (inlineCssCount > 10) {
        issues.push(`High number of inline styles (${inlineCssCount}) - consider moving to CSS file`);
    }
    
    if (!hasPreconnect) {
        issues.push('No preconnect/dns-prefetch links found - consider adding for external resources');
    }
    
    // Check for font optimization
    const fontLinks = document.querySelectorAll('link[href*="fonts.googleapis.com"]');
    let hasFontDisplay = false;
    fontLinks.forEach(link => {
        if (link.href.includes('display=swap')) {
            hasFontDisplay = true;
        }
    });
    
    if (fontLinks.length > 0 && !hasFontDisplay) {
        issues.push('Google Fonts should use display=swap for better performance');
    }
    
    metrics.externalCssCount = externalCssCount;
    metrics.externalJsCount = externalJsCount;
    metrics.inlineCssCount = inlineCssCount;
    metrics.hasPreconnect = hasPreconnect;
    metrics.hasFontDisplay = hasFontDisplay;
    
    if (issues.length > 0) {
        passed = false;
    }
    
    performanceResults.push({
        name: testName,
        passed: passed,
        issues: issues,
        metrics: metrics
    });
}

// Test Core Web Vitals metrics
function testCoreWebVitals() {
    const testName = 'Core Web Vitals';
    let passed = true;
    const issues = [];
    const metrics = {};
    
    // Use Performance API if available
    if ('performance' in window) {
        const navigation = performance.getEntriesByType('navigation')[0];
        
        if (navigation) {
            // Calculate key metrics
            const domContentLoaded = navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart;
            const loadComplete = navigation.loadEventEnd - navigation.loadEventStart;
            const totalLoadTime = navigation.loadEventEnd - navigation.fetchStart;
            
            metrics.domContentLoaded = Math.round(domContentLoaded);
            metrics.loadComplete = Math.round(loadComplete);
            metrics.totalLoadTime = Math.round(totalLoadTime);
            
            // Performance thresholds
            if (totalLoadTime > 3000) { // 3 seconds
                passed = false;
                issues.push(`Total load time is high (${Math.round(totalLoadTime)}ms) - should be under 3000ms`);
            }
            
            if (domContentLoaded > 1500) { // 1.5 seconds
                passed = false;
                issues.push(`DOM Content Loaded time is high (${Math.round(domContentLoaded)}ms) - should be under 1500ms`);
            }
        }
        
        // Check for layout shifts (simplified)
        const layoutShifts = performance.getEntriesByType('layout-shift');
        if (layoutShifts.length > 5) {
            passed = false;
            issues.push(`High number of layout shifts detected (${layoutShifts.length}) - may affect CLS`);
        }
        
        // Check resource timing
        const resources = performance.getEntriesByType('resource');
        let slowResources = 0;
        
        resources.forEach(resource => {
            const loadTime = resource.responseEnd - resource.requestStart;
            if (loadTime > 1000) { // 1 second threshold
                slowResources++;
            }
        });
        
        if (slowResources > 0) {
            issues.push(`${slowResources} resources took over 1 second to load`);
        }
        
        metrics.layoutShifts = layoutShifts.length;
        metrics.slowResources = slowResources;
        metrics.totalResources = resources.length;
    } else {
        issues.push('Performance API not available - cannot measure Core Web Vitals');
        passed = false;
    }
    
    // Check viewport meta tag
    const viewportMeta = document.querySelector('meta[name="viewport"]');
    if (!viewportMeta) {
        passed = false;
        issues.push('Missing viewport meta tag - affects mobile performance');
    }
    
    // Check for render-blocking resources
    const renderBlockingCSS = document.querySelectorAll('link[rel="stylesheet"]:not([media])');
    if (renderBlockingCSS.length > 2) {
        issues.push(`${renderBlockingCSS.length} render-blocking CSS files - consider inlining critical CSS`);
    }
    
    performanceResults.push({
        name: testName,
        passed: passed,
        issues: issues,
        metrics: metrics
    });
}

// Display performance test results
function displayPerformanceResults() {
    console.log('\n=== Performance Optimization Test Results ===');
    
    let allPassed = true;
    performanceResults.forEach(test => {
        const status = test.passed ? '✅ PASS' : '❌ FAIL';
        console.log(`${status}: ${test.name}`);
        
        if (test.metrics && Object.keys(test.metrics).length > 0) {
            console.log('  Metrics:', test.metrics);
        }
        
        if (!test.passed) {
            allPassed = false;
            test.issues.forEach(issue => {
                console.log(`  - ${issue}`);
            });
        }
    });
    
    console.log('\n=== Performance Summary ===');
    console.log(`Tests passed: ${performanceResults.filter(t => t.passed).length}/${performanceResults.length}`);
    
    if (allPassed) {
        console.log('🎉 All performance tests passed!');
    } else {
        console.log('⚠️  Some performance issues found. Consider optimizing for better user experience.');
    }
    
    // Store results globally for potential external access
    window.performanceTestResults = performanceResults;
    
    return allPassed;
}

// Export for potential use in other contexts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        runPerformanceTests,
        performanceResults
    };
}