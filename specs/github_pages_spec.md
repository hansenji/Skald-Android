# Skald - GitHub Pages Site Specification

This document outlines the visual identity, structure, page hierarchy, styling rules, and hosting configuration for the Skald application's public GitHub Pages website.

---

## 1. Purpose and Visual Identity

The website introduces Skald to potential users, highlights its features and self-hosted integration, establishes its brand personality, and hosts required legal artifacts (such as the Privacy Policy).

### A. Brand Identity
The design aligns with the Norse oral tradition defined in the [Design Specification](file:///home/hansenji/src/abs-client-app/specs/design_spec.md):
- **Logo Symbol**: A modern, sleek Viking longship (sailing ship) badge incorporating brand-colored gradients.
- **Micro-interactions**: The longship logo features a gentle rocking/floating animation, echoing the native splash screen micro-interactions of the Android client.

---

## 2. Design System & Style Guide

The website implements a responsive, highly premium dark mode layout matching the **Premium Obsidian & Electric Purple** color palette of the app.

### A. Color Palette

| Name | Hex Value | Role | CSS Variable |
| :--- | :--- | :--- | :--- |
| **Obsidian Background** | `#0B0F19` | Main page body background | `--bg-obsidian` |
| **Deep Slate Surface** | `#151D30` | Cards, hero containers, headers, and navigation bars | `--bg-slate` |
| **Slate Variant Border**| `#222E4B` | Divider borders and outlines | `--border-slate` |
| **Electric Purple Accent** | `#BB86FC` | Primary focus accent, gradients, highlights, and buttons | `--color-purple` |
| **Cyan Accent** | `#03DAC6` | Hover effects, links, inline badges, and secondary elements | `--color-cyan` |
| **Soft Pink Accent** | `#FF79C6` | Highlight highlights, warning states, and gradients | `--color-pink` |
| **Text Primary** | `#F1F5F9` | Main headers and primary content | `--text-primary` |
| **Text Secondary** | `#94A3B8` | Subheadings, descriptors, and body copy | `--text-secondary` |
| **Text Muted** | `#64748B` | Footers and copyright details | `--text-muted` |

### B. Typography
The site loads typography dynamically from Google Fonts:
- **Headings (`<h1>` to `<h3>`)**: `Outfit`, sans-serif (Weights: 600, 700). Promotes a modern, clean, and bold presentation.
- **Body Text (`<p>`, `<a>`, lists)**: `Inter`, sans-serif (Weights: 400, 500). Optimized for high readability across multiple screen sizes.
- **Monospace Code (`<code>`, `<pre>`)**: `Fira Code` or monospace.

---

## 3. Site Map & Navigation Hierarchy

The website comprises two main views:
1.  **Home Page (`/docs/index.html`)**: Introduces Skald, presents the brand identity, explains the naming lore, and links to downloads and source files.
2.  **Privacy Policy Page (`/docs/privacy.html`)**: Hosts the minimal privacy policy documentation.

### A. Global Layout Zones

#### 1. Header (Navbar)
- A sticky header with a backdrop filter (`backdrop-filter: blur(12px)`) at `rgba(15, 23, 42, 0.8)` opacity.
- **Left**: The animated Viking longship logo badge and "Skald" text.
- **Right**: Hyperlinks to "Home", "GitHub Repository" (external link), and "Privacy Policy".

#### 2. Hero Section (Home Page)
- An immersive container with a subtle radial gradient centered around the Viking longship icon.
- App Title: "Skald" in a large heading (`3.5rem` to `4.5rem` sizing) utilizing a purple-to-cyan gradient background text clip.
- Subtitle: "An Android client for Audiobookshelf, a self-hosted audiobook and podcast server. Built with Kotlin, Jetpack Compose, and modern Android development best practices."
- Call-to-Action (CTA): Prominent link button highlighting the project's source code or installation guides.

#### 3. Lore & Story Section (Home Page)
- Focuses on the Norse history of a "skald" as a storyteller:
  - Heading: "📖 What is a Skald?"
  - Paragraphs outlining:
    - *The History*: "In Old Norse culture, a skald (or skáld) was a poet, storyteller, and oral historian. Skalds travelled between courts composing elaborate poems to preserve history, share mythologies, and entertain listeners with heroic stories of old."
    - *The Modern Adaptation*: "As a modern audiobook and podcast player, Skald continues this oral tradition—acting as your digital storyteller, bringing narrated books and audio epics directly to your ears."
  - Centered layout, wrapped in a card structure with rounded corners and a slate border outline.

#### 4. Footer
- Simple copyright block, direct links to the Privacy Policy, the Audiobookshelf website, and developer email contacts.

---

## 4. Privacy Policy Requirement

To publish the application on public app distribution platforms and align with self-hosted ethos, a dedicated privacy policy page is maintained at `/docs/privacy.html`.

### A. Policy Text
Matching the Audiobookshelf zero-tracking philosophy, the statement is:
> **We don't store your data, period.**
>
> Skald is free and open source, so you can always have a look for yourself. We encourage that.

---

## 5. Interaction Design & Micro-animations

- **Viking Ship Swaying**: The primary ship icon includes a CSS keyframe animation mimicking a gentle wave swell (rotation of `-2deg` to `2deg` combined with a vertical translation of `-4px` to `4px` over a `6s` loop).
- **Smooth Hover States**: Hyperlinks, buttons, and card containers utilize transition timers (`transition: all 0.3s ease`) to fade color modifications (purple to cyan text glows) and lift/elevate elements on hover.
- **Adaptive Sizing**: Flexbox and CSS Grid adapt the header, hero sections, and lore columns cleanly between vertical mobile views and wide desktop layouts.

---

## 6. Hosting and Deployment

- Serves directly out of the `/docs` directory of the repository's `main` branch.
- Configured in the GitHub Repository settings under **Pages -> Build and deployment -> Source** set to **Deploy from a branch** -> Branch: **`main`** / Folder: **`/docs`**.
