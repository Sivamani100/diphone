from PIL import Image
import os

# Open the original image
img = Image.open('diphone.png')

# Define sizes for different densities
sizes = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}

# Create resized versions for each density
for folder, size in sizes.items():
    # Resize the image
    resized = img.resize((size, size), Image.Resampling.LANCZOS)
    
    # Save as ic_launcher.png
    path = f'app/src/main/res/{folder}/ic_launcher.png'
    resized.save(path, 'PNG')
    print(f'Created: {path} ({size}x{size})')
    
    # Save as ic_launcher_round.png
    path_round = f'app/src/main/res/{folder}/ic_launcher_round.png'
    resized.save(path_round, 'PNG')
    print(f'Created: {path_round} ({size}x{size})')

print("\nAll launcher icons created successfully!")
