window.onload = function() {
    const timeOutId = setTimeout(() => {
        executeAfterDomLoads();
        clearTimeout(timeOutId);
    }, 500);
};

// const executeAfterDomLoads = () => {
//     var profileDropdown = document.querySelector("#profile-dropdown");
//     document.addEventListener('click', (event) => {
//         const isClickInsideProfile = profileDropdown?.contains(event.target);
//         if (profileDropdown && !isClickInsideProfile) {
//             profileDropdown.classList.remove("flex");
//         }
//     });
// }
// function toggleDropdown() {
//     document.querySelector("#profile-dropdown").classList.toggle("flex");
// }