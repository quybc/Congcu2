document.addEventListener('DOMContentLoaded', function() {
    // Create container
    const container = document.createElement('div');
    container.id = 'petal-container';
    document.body.appendChild(container);

    const colors = [
        '#ffc0cb', // Pink
        '#ffb7b2', // Light Red/Pink
        '#ff9e9e', // Salmon
        '#f48fb1', // Pink 200
        '#ff80ab', // Pink Accent
        '#e91e63'  // Darker Pink (from the site banner)
    ];

    function createPetal() {
        const petal = document.createElement('div');
        petal.classList.add('petal');

        // Randomize size
        const size = Math.random() * 15 + 10; // 10px to 25px
        petal.style.width = `${size}px`;
        petal.style.height = `${size}px`;

        // Randomize position
        petal.style.left = `${Math.random() * 100}vw`;

        // Randomize animation duration (fall speed)
        const duration = Math.random() * 5 + 5; // 5s to 10s
        petal.style.animationName = 'fall';
        petal.style.animationDuration = `${duration}s`;
        petal.style.animationTimingFunction = 'linear';
        petal.style.animationFillMode = 'forwards';

        // Randomize delay
        petal.style.animationDelay = `${Math.random() * 2}s`;

        // Randomize color
        const color = colors[Math.floor(Math.random() * colors.length)];
        petal.style.background = color;

        // Randomize rotation slightly for start
        petal.style.transform = `rotate(${Math.random() * 360}deg)`;

        // Add to container
        container.appendChild(petal);

        // Remove after animation finishes
        setTimeout(() => {
            petal.remove();
        }, (duration + 2) * 1000);
    }

    // Spawn petals
    setInterval(createPetal, 300); // Create a new petal every 300ms
});
