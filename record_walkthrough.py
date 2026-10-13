import asyncio
import os
import shutil
import subprocess
from playwright.async_api import async_playwright

async def run_walkthrough():
    video_dir = '/Users/souvikkundu/Desktop/weather/temp_video_rec'
    os.makedirs(video_dir, exist_ok=True)

    print("Launching Chrome for recording...")
    async with async_playwright() as p:
        browser = await p.chromium.launch(
            executable_path='/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
            headless=True
        )
        context = await browser.new_context(
            viewport={'width': 1280, 'height': 800},
            device_scale_factor=1,
            record_video_dir=video_dir,
            record_video_size={'width': 1280, 'height': 800}
        )
        page = await context.new_page()

        print("1. Navigating to login page...")
        await page.goto('http://127.0.0.1:5173/')
        await page.wait_for_timeout(2500)

        print("2. Demonstrating 3D perspective mouse tilt...")
        for x, y in [(300, 250), (950, 250), (950, 600), (350, 580), (640, 400)]:
            await page.mouse.move(x, y, steps=18)
            await page.wait_for_timeout(250)

        print("3. Hovering over glass chips...")
        humidity_chip = page.locator('.chipHumidity')
        if await humidity_chip.count() > 0:
            box = await humidity_chip.bounding_box()
            if box:
                await page.mouse.move(box['x'] + box['width']/2, box['y'] + box['height']/2, steps=15)
                await page.wait_for_timeout(900)

        aqi_chip = page.locator('.chipAqi')
        if await aqi_chip.count() > 0:
            box = await aqi_chip.bounding_box()
            if box:
                await page.mouse.move(box['x'] + box['width']/2, box['y'] + box['height']/2, steps=15)
                await page.wait_for_timeout(900)

        print("4. Typing phone number...")
        phone_input = page.locator('#phone')
        await phone_input.click()
        await page.wait_for_timeout(400)
        await phone_input.press_sequentially('9876543210', delay=120)
        await page.wait_for_timeout(1400) # Showcase pop-in green checkmark

        print("5. Clicking Send OTP...")
        send_btn = page.locator('.sendOtpBtn')
        await send_btn.click()
        await page.wait_for_timeout(1200)

        print("6. Entering 6-digit OTP code...")
        otp_inputs = page.locator('.otpRow input')
        if await otp_inputs.count() >= 6:
            for i, digit in enumerate('123456'):
                await otp_inputs.nth(i).fill(digit)
                await page.wait_for_timeout(280)
        await page.wait_for_timeout(2000) # Wait for verification & transition to dashboard

        print("7. Dashboard view...")
        await page.wait_for_timeout(2500)

        print("8. Scrolling dashboard...")
        await page.evaluate("window.scrollTo({ top: 380, behavior: 'smooth' })")
        await page.wait_for_timeout(2000)
        await page.evaluate("window.scrollTo({ top: 0, behavior: 'smooth' })")
        await page.wait_for_timeout(1500)

        print("9. Toggling Temperature Units (Settings: °C <-> °F)...")
        settings_btn = page.locator('nav button:has-text("Settings")')
        if await settings_btn.count() > 0:
            await settings_btn.click()
            await page.wait_for_timeout(1500)
            await settings_btn.click()
            await page.wait_for_timeout(1200)

        print("10. Opening Interactive Radar / Weather Map Modal...")
        radar_btn = page.locator('nav button:has-text("Radar")')
        if await radar_btn.count() > 0:
            await radar_btn.click()
            await page.wait_for_timeout(2500)
            close_btn = page.locator('button[aria-label="Close modal"], .mapModalClose, .modalClose')
            if await close_btn.count() > 0:
                await close_btn.first.click()
            else:
                await page.keyboard.press('Escape')
            await page.wait_for_timeout(1500)

        print("11. Searching worldwide cities (Tokyo)...")
        search_input = page.locator('.search input')
        if await search_input.count() > 0:
            await search_input.click()
            await search_input.fill('Tokyo')
            await page.wait_for_timeout(1200)
            first_suggest = page.locator('.searchSuggest button, .searchItem')
            if await first_suggest.count() > 0:
                await first_suggest.first.click()
            else:
                await search_input.press('Enter')
            await page.wait_for_timeout(3000)

        print("12. Searching worldwide cities (London)...")
        if await search_input.count() > 0:
            await search_input.click()
            await search_input.fill('London')
            await page.wait_for_timeout(1200)
            first_suggest = page.locator('.searchSuggest button, .searchItem')
            if await first_suggest.count() > 0:
                await first_suggest.first.click()
            else:
                await search_input.press('Enter')
            await page.wait_for_timeout(3000)

        print("13. Final showcase pause...")
        await page.wait_for_timeout(2500)

        print("Closing page to finish recording...")
        await page.close()
        await context.close()
        await browser.close()

    raw_files = [os.path.join(video_dir, f) for f in os.listdir(video_dir) if f.endswith('.webm')]
    if not raw_files:
        print("Error: No recorded video file found!")
        return

    raw_video = raw_files[0]
    out_mp4 = '/Users/souvikkundu/Desktop/weather/weatherly_walkthrough.mp4'
    artifact_mp4 = '/Users/souvikkundu/.gemini/antigravity-ide/brain/45889949-9a88-49fb-a177-9b1067e8eb4f/weatherly_walkthrough.mp4'

    print(f"Transcoding {raw_video} to high-definition MP4...")
    cmd = [
        '/opt/homebrew/bin/ffmpeg',
        '-y',
        '-i', raw_video,
        '-c:v', 'libx264',
        '-preset', 'slow',
        '-crf', '18',
        '-pix_fmt', 'yuv420p',
        out_mp4
    ]
    subprocess.run(cmd, check=True)
    shutil.copy2(out_mp4, artifact_mp4)
    print(f"Video saved to {out_mp4} and {artifact_mp4}")

    # Clean up temp webm
    shutil.rmtree(video_dir, ignore_errors=True)

if __name__ == '__main__':
    asyncio.run(run_walkthrough())
