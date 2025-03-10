package net.minecraft.client.renderer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityRainFX;
import net.minecraft.client.particle.EntitySmokeFX;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MouseFilter;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Vec3;
import net.minecraft.world.biome.BiomeGenBase;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.GLU;

import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

@SideOnly(Side.CLIENT)
public class EntityRenderer
{
    public static boolean anaglyphEnable = false;

    /** Anaglyph field (0=R, 1=GB) */
    public static int anaglyphField;

    /** A reference to the Minecraft object. */
    private Minecraft mc;
    private float farPlaneDistance = 0.0F;
    public ItemRenderer itemRenderer;

    /** Entity renderer update count */
    private int rendererUpdateCount;

    /** Pointed entity */
    private Entity pointedEntity = null;
    private MouseFilter mouseFilterXAxis = new MouseFilter();
    private MouseFilter mouseFilterYAxis = new MouseFilter();

    /** Mouse filter dummy 1 */
    private MouseFilter mouseFilterDummy1 = new MouseFilter();

    /** Mouse filter dummy 2 */
    private MouseFilter mouseFilterDummy2 = new MouseFilter();

    /** Mouse filter dummy 3 */
    private MouseFilter mouseFilterDummy3 = new MouseFilter();

    /** Mouse filter dummy 4 */
    private MouseFilter mouseFilterDummy4 = new MouseFilter();
    private float thirdPersonDistance = 4.0F;

    /** Third person distance temp */
    private float thirdPersonDistanceTemp = 4.0F;
    private float debugCamYaw = 0.0F;
    private float prevDebugCamYaw = 0.0F;
    private float debugCamPitch = 0.0F;
    private float prevDebugCamPitch = 0.0F;

    /** Smooth cam yaw */
    private float smoothCamYaw;

    /** Smooth cam pitch */
    private float smoothCamPitch;

    /** Smooth cam filter X */
    private float smoothCamFilterX;

    /** Smooth cam filter Y */
    private float smoothCamFilterY;

    /** Smooth cam partial ticks */
    private float smoothCamPartialTicks;
    private float debugCamFOV = 0.0F;
    private float prevDebugCamFOV = 0.0F;
    private float camRoll = 0.0F;
    private float prevCamRoll = 0.0F;

    /**
     * The texture id of the blocklight/skylight texture used for lighting effects
     */
    public int lightmapTexture;

    /**
     * Colors computed in updateLightmap() and loaded into the lightmap emptyTexture
     */
    private int[] lightmapColors;

    /** FOV modifier hand */
    private float fovModifierHand;

    /** FOV modifier hand prev */
    private float fovModifierHandPrev;

    /** FOV multiplier temp */
    private float fovMultiplierTemp;
    private float field_82831_U;
    private float field_82832_V;

    /** Cloud fog mode */
    private boolean cloudFog = false;
    private double cameraZoom = 1.0D;
    private double cameraYaw = 0.0D;
    private double cameraPitch = 0.0D;

    /** Previous frame time in milliseconds */
    private long prevFrameTime = Minecraft.getSystemTime();

    /** End time of last render (ns) */
    private long renderEndNanoTime = 0L;

    /**
     * Is set, updateCameraAndRender() calls updateLightmap(); set by updateTorchFlicker()
     */
    private boolean lightmapUpdateNeeded = false;

    /** Torch flicker X */
    float torchFlickerX = 0.0F;

    /** Torch flicker DX */
    float torchFlickerDX = 0.0F;

    /** Torch flicker Y */
    float torchFlickerY = 0.0F;

    /** Torch flicker DY */
    float torchFlickerDY = 0.0F;
    private Random random = new Random();

    /** Rain sound counter */
    private int rainSoundCounter = 0;

    /** Rain X coords */
    float[] rainXCoords;

    /** Rain Y coords */
    float[] rainYCoords;
    volatile int field_78523_k = 0;
    volatile int field_78520_l = 0;

    /** Fog color buffer */
    FloatBuffer fogColorBuffer = GLAllocation.createDirectFloatBuffer(16);

    /** red component of the fog color */
    float fogColorRed;

    /** green component of the fog color */
    float fogColorGreen;

    /** blue component of the fog color */
    float fogColorBlue;

    /** Fog color 2 */
    private float fogColor2;

    /** Fog color 1 */
    private float fogColor1;

    /**
     * Debug view direction (0=OFF, 1=Front, 2=Right, 3=Back, 4=Left, 5=TiltLeft, 6=TiltRight)
     */
    public int debugViewDirection;

    public EntityRenderer(Minecraft par1Minecraft)
    {
        this.mc = par1Minecraft;
        this.itemRenderer = new ItemRenderer(par1Minecraft);
        this.lightmapTexture = par1Minecraft.renderEngine.allocateAndSetupTexture(new BufferedImage(16, 16, 1));
        this.lightmapColors = new int[256];
    }

    /**
     * Updates the entity renderer
     */
    public void updateRenderer()
    {
        this.updateFovModifierHand();
        this.updateTorchFlicker();
        this.fogColor2 = this.fogColor1;
        this.thirdPersonDistanceTemp = this.thirdPersonDistance;
        this.prevDebugCamYaw = this.debugCamYaw;
        this.prevDebugCamPitch = this.debugCamPitch;
        this.prevDebugCamFOV = this.debugCamFOV;
        this.prevCamRoll = this.camRoll;
        float f;
        float f1;

        if (this.mc.gameSettings.smoothCamera)
        {
            f = this.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            f1 = f * f * f * 8.0F;
            this.smoothCamFilterX = this.mouseFilterXAxis.smooth(this.smoothCamYaw, 0.05F * f1);
            this.smoothCamFilterY = this.mouseFilterYAxis.smooth(this.smoothCamPitch, 0.05F * f1);
            this.smoothCamPartialTicks = 0.0F;
            this.smoothCamYaw = 0.0F;
            this.smoothCamPitch = 0.0F;
        }

        if (this.mc.renderViewEntity == null)
        {
            this.mc.renderViewEntity = this.mc.thePlayer;
        }

        f = this.mc.theWorld.getLightBrightness(MathHelper.floor_double(this.mc.renderViewEntity.posX), MathHelper.floor_double(this.mc.renderViewEntity.posY), MathHelper.floor_double(this.mc.renderViewEntity.posZ));
        f1 = (float)(3 - this.mc.gameSettings.renderDistance) / 3.0F;
        float f2 = f * (1.0F - f1) + f1;
        this.fogColor1 += (f2 - this.fogColor1) * 0.1F;
        ++this.rendererUpdateCount;
        this.itemRenderer.updateEquippedItem();
        this.addRainParticles();
        this.field_82832_V = this.field_82831_U;

        if (BossStatus.field_82825_d)
        {
            this.field_82831_U += 0.05F;

            if (this.field_82831_U > 1.0F)
            {
                this.field_82831_U = 1.0F;
            }

            BossStatus.field_82825_d = false;
        }
        else if (this.field_82831_U > 0.0F)
        {
            this.field_82831_U -= 0.0125F;
        }
    }

    /**
     * Finds what block or object the mouse is over at the specified partial tick time. Args: partialTickTime
     */
    public void getMouseOver(float par1)
    {
        if (this.mc.renderViewEntity != null)
        {
            if (this.mc.theWorld != null)
            {
                this.mc.pointedEntityLiving = null;
                double d0 = (double)this.mc.playerController.getBlockReachDistance();
                this.mc.objectMouseOver = this.mc.renderViewEntity.rayTrace(d0, par1);
                double d1 = d0;
                Vec3 vec3 = this.mc.renderViewEntity.getPosition(par1);

                if (this.mc.playerController.extendedReach())
                {
                    d0 = 6.0D;
                    d1 = 6.0D;
                }
                else
                {
                    if (d0 > 3.0D)
                    {
                        d1 = 3.0D;
                    }

                    d0 = d1;
                }

                if (this.mc.objectMouseOver != null)
                {
                    d1 = this.mc.objectMouseOver.hitVec.distanceTo(vec3);
                }

                Vec3 vec31 = this.mc.renderViewEntity.getLook(par1);
                Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
                this.pointedEntity = null;
                float f1 = 1.0F;
                List list = this.mc.theWorld.getEntitiesWithinAABBExcludingEntity(this.mc.renderViewEntity, this.mc.renderViewEntity.boundingBox.addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand((double)f1, (double)f1, (double)f1));
                double d2 = d1;

                for (int i = 0; i < list.size(); ++i)
                {
                    Entity entity = (Entity)list.get(i);

                    if (entity.canBeCollidedWith())
                    {
                        float f2 = entity.getCollisionBorderSize();
                        AxisAlignedBB axisalignedbb = entity.boundingBox.expand((double)f2, (double)f2, (double)f2);
                        MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

                        if (axisalignedbb.isVecInside(vec3))
                        {
                            if (0.0D < d2 || d2 == 0.0D)
                            {
                                this.pointedEntity = entity;
                                d2 = 0.0D;
                            }
                        }
                        else if (movingobjectposition != null)
                        {
                            double d3 = vec3.distanceTo(movingobjectposition.hitVec);

                            if (d3 < d2 || d2 == 0.0D)
                            {
                                this.pointedEntity = entity;
                                d2 = d3;
                            }
                        }
                    }
                }

                if (this.pointedEntity != null && (d2 < d1 || this.mc.objectMouseOver == null))
                {
                    this.mc.objectMouseOver = new MovingObjectPosition(this.pointedEntity);

                    if (this.pointedEntity instanceof EntityLiving)
                    {
                        this.mc.pointedEntityLiving = (EntityLiving)this.pointedEntity;
                    }
                }
            }
        }
    }

    /**
     * Update FOV modifier hand
     */
    private void updateFovModifierHand()
    {
        if (mc.renderViewEntity instanceof EntityPlayerSP)
        {
            EntityPlayerSP entityplayersp = (EntityPlayerSP)this.mc.renderViewEntity;
            this.fovMultiplierTemp = entityplayersp.getFOVMultiplier();
        }
        else
        {
            this.fovMultiplierTemp = mc.thePlayer.getFOVMultiplier();
        }
        this.fovModifierHandPrev = this.fovModifierHand;
        this.fovModifierHand += (this.fovMultiplierTemp - this.fovModifierHand) * 0.5F;

        if (this.fovModifierHand > 1.5F)
        {
            this.fovModifierHand = 1.5F;
        }

        if (this.fovModifierHand < 0.1F)
        {
            this.fovModifierHand = 0.1F;
        }
    }

    /**
     * Changes the field of view of the player depending on if they are underwater or not
     */
    private float getFOVModifier(float par1, boolean par2)
    {
        if (this.debugViewDirection > 0)
        {
            return 90.0F;
        }
        else
        {
            EntityLiving entityplayer = (EntityLiving)this.mc.renderViewEntity;
            float f1 = 70.0F;

            if (par2)
            {
                f1 += this.mc.gameSettings.fovSetting * 40.0F;
                f1 *= this.fovModifierHandPrev + (this.fovModifierHand - this.fovModifierHandPrev) * par1;
            }

            if (entityplayer.getHealth() <= 0)
            {
                float f2 = (float)entityplayer.deathTime + par1;
                f1 /= (1.0F - 500.0F / (f2 + 500.0F)) * 2.0F + 1.0F;
            }

            int i = ActiveRenderInfo.getBlockIdAtEntityViewpoint(this.mc.theWorld, entityplayer, par1);

            if (i != 0 && Block.blocksList[i].blockMaterial == Material.water)
            {
                f1 = f1 * 60.0F / 70.0F;
            }

            return f1 + this.prevDebugCamFOV + (this.debugCamFOV - this.prevDebugCamFOV) * par1;
        }
    }

    private void hurtCameraEffect(float par1)
    {
        EntityLiving entityliving = this.mc.renderViewEntity;
        float f1 = (float)entityliving.hurtTime - par1;
        float f2;

        if (entityliving.getHealth() <= 0)
        {
            f2 = (float)entityliving.deathTime + par1;
            GL11.glRotatef(40.0F - 8000.0F / (f2 + 200.0F), 0.0F, 0.0F, 1.0F);
        }

        if (f1 >= 0.0F)
        {
            f1 /= (float)entityliving.maxHurtTime;
            f1 = MathHelper.sin(f1 * f1 * f1 * f1 * (float)Math.PI);
            f2 = entityliving.attackedAtYaw;
            GL11.glRotatef(-f2, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-f1 * 14.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(f2, 0.0F, 1.0F, 0.0F);
        }
    }

    /**
     * Setups all the GL settings for view bobbing. Args: partialTickTime
     */
    private void setupViewBobbing(float par1)
    {
        if (this.mc.renderViewEntity instanceof EntityPlayer)
        {
            EntityPlayer entityplayer = (EntityPlayer)this.mc.renderViewEntity;
            float f1 = entityplayer.distanceWalkedModified - entityplayer.prevDistanceWalkedModified;
            float f2 = -(entityplayer.distanceWalkedModified + f1 * par1);
            float f3 = entityplayer.prevCameraYaw + (entityplayer.cameraYaw - entityplayer.prevCameraYaw) * par1;
            float f4 = entityplayer.prevCameraPitch + (entityplayer.cameraPitch - entityplayer.prevCameraPitch) * par1;
            GL11.glTranslatef(MathHelper.sin(f2 * (float)Math.PI) * f3 * 0.5F, -Math.abs(MathHelper.cos(f2 * (float)Math.PI) * f3), 0.0F);
            GL11.glRotatef(MathHelper.sin(f2 * (float)Math.PI) * f3 * 3.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(Math.abs(MathHelper.cos(f2 * (float)Math.PI - 0.2F) * f3) * 5.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(f4, 1.0F, 0.0F, 0.0F);
        }
    }

    /**
     * sets up player's eye (or camera in third person mode)
     */
    private void orientCamera(float par1)
    {
        EntityLiving entityliving = this.mc.renderViewEntity;
        float f1 = entityliving.yOffset - 1.62F;
        double d0 = entityliving.prevPosX + (entityliving.posX - entityliving.prevPosX) * (double)par1;
        double d1 = entityliving.prevPosY + (entityliving.posY - entityliving.prevPosY) * (double)par1 - (double)f1;
        double d2 = entityliving.prevPosZ + (entityliving.posZ - entityliving.prevPosZ) * (double)par1;
        GL11.glRotatef(this.prevCamRoll + (this.camRoll - this.prevCamRoll) * par1, 0.0F, 0.0F, 1.0F);

        if (entityliving.isPlayerSleeping())
        {
            f1 = (float)((double)f1 + 1.0D);
            GL11.glTranslatef(0.0F, 0.3F, 0.0F);

            if (!this.mc.gameSettings.debugCamEnable)
            {
                ForgeHooksClient.orientBedCamera(mc, entityliving);
                GL11.glRotatef(entityliving.prevRotationYaw + (entityliving.rotationYaw - entityliving.prevRotationYaw) * par1 + 180.0F, 0.0F, -1.0F, 0.0F);
                GL11.glRotatef(entityliving.prevRotationPitch + (entityliving.rotationPitch - entityliving.prevRotationPitch) * par1, -1.0F, 0.0F, 0.0F);
            }
        }
        else if (this.mc.gameSettings.thirdPersonView > 0)
        {
            double d3 = (double)(this.thirdPersonDistanceTemp + (this.thirdPersonDistance - this.thirdPersonDistanceTemp) * par1);
            float f2;
            float f3;

            if (this.mc.gameSettings.debugCamEnable)
            {
                f3 = this.prevDebugCamYaw + (this.debugCamYaw - this.prevDebugCamYaw) * par1;
                f2 = this.prevDebugCamPitch + (this.debugCamPitch - this.prevDebugCamPitch) * par1;
                GL11.glTranslatef(0.0F, 0.0F, (float)(-d3));
                GL11.glRotatef(f2, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(f3, 0.0F, 1.0F, 0.0F);
            }
            else
            {
                f3 = entityliving.rotationYaw;
                f2 = entityliving.rotationPitch;

                if (this.mc.gameSettings.thirdPersonView == 2)
                {
                    f2 += 180.0F;
                }

                double d4 = (double)(-MathHelper.sin(f3 / 180.0F * (float)Math.PI) * MathHelper.cos(f2 / 180.0F * (float)Math.PI)) * d3;
                double d5 = (double)(MathHelper.cos(f3 / 180.0F * (float)Math.PI) * MathHelper.cos(f2 / 180.0F * (float)Math.PI)) * d3;
                double d6 = (double)(-MathHelper.sin(f2 / 180.0F * (float)Math.PI)) * d3;

                for (int l = 0; l < 8; ++l)
                {
                    float f4 = (float)((l & 1) * 2 - 1);
                    float f5 = (float)((l >> 1 & 1) * 2 - 1);
                    float f6 = (float)((l >> 2 & 1) * 2 - 1);
                    f4 *= 0.1F;
                    f5 *= 0.1F;
                    f6 *= 0.1F;
                    MovingObjectPosition movingobjectposition = this.mc.theWorld.rayTraceBlocks(this.mc.theWorld.getWorldVec3Pool().getVecFromPool(d0 + (double)f4, d1 + (double)f5, d2 + (double)f6), this.mc.theWorld.getWorldVec3Pool().getVecFromPool(d0 - d4 + (double)f4 + (double)f6, d1 - d6 + (double)f5, d2 - d5 + (double)f6));

                    if (movingobjectposition != null)
                    {
                        double d7 = movingobjectposition.hitVec.distanceTo(this.mc.theWorld.getWorldVec3Pool().getVecFromPool(d0, d1, d2));

                        if (d7 < d3)
                        {
                            d3 = d7;
                        }
                    }
                }

                if (this.mc.gameSettings.thirdPersonView == 2)
                {
                    GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
                }

                GL11.glRotatef(entityliving.rotationPitch - f2, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(entityliving.rotationYaw - f3, 0.0F, 1.0F, 0.0F);
                GL11.glTranslatef(0.0F, 0.0F, (float)(-d3));
                GL11.glRotatef(f3 - entityliving.rotationYaw, 0.0F, 1.0F, 0.0F);
                GL11.glRotatef(f2 - entityliving.rotationPitch, 1.0F, 0.0F, 0.0F);
            }
        }
        else
        {
            GL11.glTranslatef(0.0F, 0.0F, -0.1F);
        }

        if (!this.mc.gameSettings.debugCamEnable)
        {
            GL11.glRotatef(entityliving.prevRotationPitch + (entityliving.rotationPitch - entityliving.prevRotationPitch) * par1, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(entityliving.prevRotationYaw + (entityliving.rotationYaw - entityliving.prevRotationYaw) * par1 + 180.0F, 0.0F, 1.0F, 0.0F);
        }

        GL11.glTranslatef(0.0F, f1, 0.0F);
        d0 = entityliving.prevPosX + (entityliving.posX - entityliving.prevPosX) * (double)par1;
        d1 = entityliving.prevPosY + (entityliving.posY - entityliving.prevPosY) * (double)par1 - (double)f1;
        d2 = entityliving.prevPosZ + (entityliving.posZ - entityliving.prevPosZ) * (double)par1;
        this.cloudFog = this.mc.renderGlobal.hasCloudFog(d0, d1, d2, par1);
    }

    /**
     * sets up projection, view effects, camera position/rotation
     */
    private void setupCameraTransform(float par1, int par2)
    {
        this.farPlaneDistance = (float)(256 >> this.mc.gameSettings.renderDistance);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        float f1 = 0.07F;

        if (this.mc.gameSettings.anaglyph)
        {
            GL11.glTranslatef((float)(-(par2 * 2 - 1)) * f1, 0.0F, 0.0F);
        }

        if (this.cameraZoom != 1.0D)
        {
            GL11.glTranslatef((float)this.cameraYaw, (float)(-this.cameraPitch), 0.0F);
            GL11.glScaled(this.cameraZoom, this.cameraZoom, 1.0D);
        }

        GLU.gluPerspective(this.getFOVModifier(par1, true), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.farPlaneDistance * 2.0F);
        float f2;

        if (this.mc.playerController.enableEverythingIsScrewedUpMode())
        {
            f2 = 0.6666667F;
            GL11.glScalef(1.0F, f2, 1.0F);
        }

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        if (this.mc.gameSettings.anaglyph)
        {
            GL11.glTranslatef((float)(par2 * 2 - 1) * 0.1F, 0.0F, 0.0F);
        }

        this.hurtCameraEffect(par1);

        if (this.mc.gameSettings.viewBobbing)
        {
            this.setupViewBobbing(par1);
        }

        f2 = this.mc.thePlayer.prevTimeInPortal + (this.mc.thePlayer.timeInPortal - this.mc.thePlayer.prevTimeInPortal) * par1;

        if (f2 > 0.0F)
        {
            byte b0 = 20;

            if (this.mc.thePlayer.isPotionActive(Potion.confusion))
            {
                b0 = 7;
            }

            float f3 = 5.0F / (f2 * f2 + 5.0F) - f2 * 0.04F;
            f3 *= f3;
            GL11.glRotatef(((float)this.rendererUpdateCount + par1) * (float)b0, 0.0F, 1.0F, 1.0F);
            GL11.glScalef(1.0F / f3, 1.0F, 1.0F);
            GL11.glRotatef(-((float)this.rendererUpdateCount + par1) * (float)b0, 0.0F, 1.0F, 1.0F);
        }

        this.orientCamera(par1);

        if (this.debugViewDirection > 0)
        {
            int j = this.debugViewDirection - 1;

            if (j == 1)
            {
                GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            }

            if (j == 2)
            {
                GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            }

            if (j == 3)
            {
                GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
            }

            if (j == 4)
            {
                GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
            }

            if (j == 5)
            {
                GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            }
        }
    }

    /**
     * Render player hand
     */
    private void renderHand(float par1, int par2)
    {
        if (this.debugViewDirection <= 0)
        {
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            float f1 = 0.07F;

            if (this.mc.gameSettings.anaglyph)
            {
                GL11.glTranslatef((float)(-(par2 * 2 - 1)) * f1, 0.0F, 0.0F);
            }

            if (this.cameraZoom != 1.0D)
            {
                GL11.glTranslatef((float)this.cameraYaw, (float)(-this.cameraPitch), 0.0F);
                GL11.glScaled(this.cameraZoom, this.cameraZoom, 1.0D);
            }

            GLU.gluPerspective(this.getFOVModifier(par1, false), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.farPlaneDistance * 2.0F);

            if (this.mc.playerController.enableEverythingIsScrewedUpMode())
            {
                float f2 = 0.6666667F;
                GL11.glScalef(1.0F, f2, 1.0F);
            }

            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();

            if (this.mc.gameSettings.anaglyph)
            {
                GL11.glTranslatef((float)(par2 * 2 - 1) * 0.1F, 0.0F, 0.0F);
            }

            GL11.glPushMatrix();
            this.hurtCameraEffect(par1);

            if (this.mc.gameSettings.viewBobbing)
            {
                this.setupViewBobbing(par1);
            }

            if (this.mc.gameSettings.thirdPersonView == 0 && !this.mc.renderViewEntity.isPlayerSleeping() && !this.mc.gameSettings.hideGUI && !this.mc.playerController.enableEverythingIsScrewedUpMode())
            {
                this.enableLightmap((double)par1);
                this.itemRenderer.renderItemInFirstPerson(par1);
                this.disableLightmap((double)par1);
            }

            GL11.glPopMatrix();

            if (this.mc.gameSettings.thirdPersonView == 0 && !this.mc.renderViewEntity.isPlayerSleeping())
            {
                this.itemRenderer.renderOverlays(par1);
                this.hurtCameraEffect(par1);
            }

            if (this.mc.gameSettings.viewBobbing)
            {
                this.setupViewBobbing(par1);
            }
        }
    }

    /**
     * Disable secondary texture unit used by lightmap
     */
    public void disableLightmap(double par1)
    {
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    /**
     * Enable lightmap in secondary texture unit
     */
    public void enableLightmap(double par1)
    {
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glLoadIdentity();
        float f = 0.00390625F;
        GL11.glScalef(f, f, f);
        GL11.glTranslatef(8.0F, 8.0F, 8.0F);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.lightmapTexture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        this.mc.renderEngine.resetBoundTexture();
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    /**
     * Recompute a random value that is applied to block color in updateLightmap()
     */
    private void updateTorchFlicker()
    {
        this.torchFlickerDX = (float)((double)this.torchFlickerDX + (Math.random() - Math.random()) * Math.random() * Math.random());
        this.torchFlickerDY = (float)((double)this.torchFlickerDY + (Math.random() - Math.random()) * Math.random() * Math.random());
        this.torchFlickerDX = (float)((double)this.torchFlickerDX * 0.9D);
        this.torchFlickerDY = (float)((double)this.torchFlickerDY * 0.9D);
        this.torchFlickerX += (this.torchFlickerDX - this.torchFlickerX) * 1.0F;
        this.torchFlickerY += (this.torchFlickerDY - this.torchFlickerY) * 1.0F;
        this.lightmapUpdateNeeded = true;
    }

    private void updateLightmap(float par1)
    {
        WorldClient worldclient = this.mc.theWorld;

        if (worldclient != null)
        {
            for (int i = 0; i < 256; ++i)
            {
                float f1 = worldclient.getSunBrightness(1.0F) * 0.95F + 0.05F;
                float f2 = worldclient.provider.lightBrightnessTable[i / 16] * f1;
                float f3 = worldclient.provider.lightBrightnessTable[i % 16] * (this.torchFlickerX * 0.1F + 1.5F);

                if (worldclient.lastLightningBolt > 0)
                {
                    f2 = worldclient.provider.lightBrightnessTable[i / 16];
                }

                float f4 = f2 * (worldclient.getSunBrightness(1.0F) * 0.65F + 0.35F);
                float f5 = f2 * (worldclient.getSunBrightness(1.0F) * 0.65F + 0.35F);
                float f6 = f3 * ((f3 * 0.6F + 0.4F) * 0.6F + 0.4F);
                float f7 = f3 * (f3 * f3 * 0.6F + 0.4F);
                float f8 = f4 + f3;
                float f9 = f5 + f6;
                float f10 = f2 + f7;
                f8 = f8 * 0.96F + 0.03F;
                f9 = f9 * 0.96F + 0.03F;
                f10 = f10 * 0.96F + 0.03F;
                float f11;

                if (this.field_82831_U > 0.0F)
                {
                    f11 = this.field_82832_V + (this.field_82831_U - this.field_82832_V) * par1;
                    f8 = f8 * (1.0F - f11) + f8 * 0.7F * f11;
                    f9 = f9 * (1.0F - f11) + f9 * 0.6F * f11;
                    f10 = f10 * (1.0F - f11) + f10 * 0.6F * f11;
                }

                if (worldclient.provider.dimensionId == 1)
                {
                    f8 = 0.22F + f3 * 0.75F;
                    f9 = 0.28F + f6 * 0.75F;
                    f10 = 0.25F + f7 * 0.75F;
                }

                float f12;

                if (this.mc.thePlayer.isPotionActive(Potion.nightVision))
                {
                    f11 = this.getNightVisionBrightness(this.mc.thePlayer, par1);
                    f12 = 1.0F / f8;

                    if (f12 > 1.0F / f9)
                    {
                        f12 = 1.0F / f9;
                    }

                    if (f12 > 1.0F / f10)
                    {
                        f12 = 1.0F / f10;
                    }

                    f8 = f8 * (1.0F - f11) + f8 * f12 * f11;
                    f9 = f9 * (1.0F - f11) + f9 * f12 * f11;
                    f10 = f10 * (1.0F - f11) + f10 * f12 * f11;
                }

                if (f8 > 1.0F)
                {
                    f8 = 1.0F;
                }

                if (f9 > 1.0F)
                {
                    f9 = 1.0F;
                }

                if (f10 > 1.0F)
                {
                    f10 = 1.0F;
                }

                f11 = this.mc.gameSettings.gammaSetting;
                f12 = 1.0F - f8;
                float f13 = 1.0F - f9;
                float f14 = 1.0F - f10;
                f12 = 1.0F - f12 * f12 * f12 * f12;
                f13 = 1.0F - f13 * f13 * f13 * f13;
                f14 = 1.0F - f14 * f14 * f14 * f14;
                f8 = f8 * (1.0F - f11) + f12 * f11;
                f9 = f9 * (1.0F - f11) + f13 * f11;
                f10 = f10 * (1.0F - f11) + f14 * f11;
                f8 = f8 * 0.96F + 0.03F;
                f9 = f9 * 0.96F + 0.03F;
                f10 = f10 * 0.96F + 0.03F;

                if (f8 > 1.0F)
                {
                    f8 = 1.0F;
                }

                if (f9 > 1.0F)
                {
                    f9 = 1.0F;
                }

                if (f10 > 1.0F)
                {
                    f10 = 1.0F;
                }

                if (f8 < 0.0F)
                {
                    f8 = 0.0F;
                }

                if (f9 < 0.0F)
                {
                    f9 = 0.0F;
                }

                if (f10 < 0.0F)
                {
                    f10 = 0.0F;
                }

                short short1 = 255;
                int j = (int)(f8 * 255.0F);
                int k = (int)(f9 * 255.0F);
                int l = (int)(f10 * 255.0F);
                this.lightmapColors[i] = short1 << 24 | j << 16 | k << 8 | l;
            }

            this.mc.renderEngine.createTextureFromBytes(this.lightmapColors, 16, 16, this.lightmapTexture);
        }
    }

    /**
     * Gets the night vision brightness
     */
    private float getNightVisionBrightness(EntityPlayer par1EntityPlayer, float par2)
    {
        int i = par1EntityPlayer.getActivePotionEffect(Potion.nightVision).getDuration();
        return i > 200 ? 1.0F : 0.7F + MathHelper.sin(((float)i - par2) * (float)Math.PI * 0.2F) * 0.3F;
    }

    /**
     * Will update any inputs that effect the camera angle (mouse) and then render the world and GUI
     */
    public void updateCameraAndRender(float par1)
    {
        this.mc.mcProfiler.startSection("lightTex");

        if (this.lightmapUpdateNeeded)
        {
            this.updateLightmap(par1);
        }

        this.mc.mcProfiler.endSection();
        boolean flag = Display.isActive();

        if (!flag && this.mc.gameSettings.pauseOnLostFocus && (!this.mc.gameSettings.touchscreen || !Mouse.isButtonDown(1)))
        {
            if (Minecraft.getSystemTime() - this.prevFrameTime > 500L)
            {
                this.mc.displayInGameMenu();
            }
        }
        else
        {
            this.prevFrameTime = Minecraft.getSystemTime();
        }

        this.mc.mcProfiler.startSection("mouse");

        if (this.mc.inGameHasFocus && flag)
        {
            this.mc.mouseHelper.mouseXYChange();
            float f1 = this.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float f2 = f1 * f1 * f1 * 8.0F;
            float f3 = (float)this.mc.mouseHelper.deltaX * f2;
            float f4 = (float)this.mc.mouseHelper.deltaY * f2;
            byte b0 = 1;

            if (this.mc.gameSettings.invertMouse)
            {
                b0 = -1;
            }

            if (this.mc.gameSettings.smoothCamera)
            {
                this.smoothCamYaw += f3;
                this.smoothCamPitch += f4;
                float f5 = par1 - this.smoothCamPartialTicks;
                this.smoothCamPartialTicks = par1;
                f3 = this.smoothCamFilterX * f5;
                f4 = this.smoothCamFilterY * f5;
                this.mc.thePlayer.setAngles(f3, f4 * (float)b0);
            }
            else
            {
                this.mc.thePlayer.setAngles(f3, f4 * (float)b0);
            }
        }

        this.mc.mcProfiler.endSection();

        if (!this.mc.skipRenderWorld)
        {
            anaglyphEnable = this.mc.gameSettings.anaglyph;
            ScaledResolution scaledresolution = new ScaledResolution(this.mc.gameSettings, this.mc.displayWidth, this.mc.displayHeight);
            int i = scaledresolution.getScaledWidth();
            int j = scaledresolution.getScaledHeight();
            int k = Mouse.getX() * i / this.mc.displayWidth;
            int l = j - Mouse.getY() * j / this.mc.displayHeight - 1;
            int i1 = performanceToFps(this.mc.gameSettings.limitFramerate);

            if (this.mc.theWorld != null)
            {
                this.mc.mcProfiler.startSection("level");

                if (this.mc.gameSettings.limitFramerate == 0)
                {
                    this.renderWorld(par1, 0L);
                }
                else
                {
                    this.renderWorld(par1, this.renderEndNanoTime + (long)(1000000000 / i1));
                }

                this.renderEndNanoTime = System.nanoTime();
                this.mc.mcProfiler.endStartSection("gui");

                if (!this.mc.gameSettings.hideGUI || this.mc.currentScreen != null)
                {
                    this.mc.ingameGUI.renderGameOverlay(par1, this.mc.currentScreen != null, k, l);
                }

                this.mc.mcProfiler.endSection();
            }
            else
            {
                GL11.glViewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glLoadIdentity();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glLoadIdentity();
                this.setupOverlayRendering();
                this.renderEndNanoTime = System.nanoTime();
            }

            if (this.mc.currentScreen != null)
            {
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

                try
                {
                    this.mc.currentScreen.drawScreen(k, l, par1);
                }
                catch (Throwable throwable)
                {
                    CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Rendering screen");
                    CrashReportCategory crashreportcategory = crashreport.makeCategory("Screen render details");
                    crashreportcategory.addCrashSectionCallable("Screen name", new CallableScreenName(this));
                    crashreportcategory.addCrashSectionCallable("Mouse location", new CallableMouseLocation(this, k, l));
                    crashreportcategory.addCrashSectionCallable("Screen size", new CallableScreenSize(this, scaledresolution));
                    throw new ReportedException(crashreport);
                }

                if (this.mc.currentScreen != null && this.mc.currentScreen.guiParticles != null)
                {
                    this.mc.currentScreen.guiParticles.draw(par1);
                }
            }
        }
    }

    public void renderWorld(float par1, long par2)
    {
        this.mc.mcProfiler.startSection("lightTex");

        if (this.lightmapUpdateNeeded)
        {
            this.updateLightmap(par1);
        }

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        if (this.mc.renderViewEntity == null)
        {
            this.mc.renderViewEntity = this.mc.thePlayer;
        }

        this.mc.mcProfiler.endStartSection("pick");
        this.getMouseOver(par1);
        EntityLiving entityliving = this.mc.renderViewEntity;
        RenderGlobal renderglobal = this.mc.renderGlobal;
        EffectRenderer effectrenderer = this.mc.effectRenderer;
        double d0 = entityliving.lastTickPosX + (entityliving.posX - entityliving.lastTickPosX) * (double)par1;
        double d1 = entityliving.lastTickPosY + (entityliving.posY - entityliving.lastTickPosY) * (double)par1;
        double d2 = entityliving.lastTickPosZ + (entityliving.posZ - entityliving.lastTickPosZ) * (double)par1;
        this.mc.mcProfiler.endStartSection("center");

        for (int j = 0; j < 2; ++j)
        {
            if (this.mc.gameSettings.anaglyph)
            {
                anaglyphField = j;

                if (anaglyphField == 0)
                {
                    GL11.glColorMask(false, true, true, false);
                }
                else
                {
                    GL11.glColorMask(true, false, false, false);
                }
            }

            this.mc.mcProfiler.endStartSection("clear");
            GL11.glViewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
            this.updateFogColor(par1);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glEnable(GL11.GL_CULL_FACE);
            this.mc.mcProfiler.endStartSection("camera");
            this.setupCameraTransform(par1, j);
            ActiveRenderInfo.updateRenderInfo(this.mc.thePlayer, this.mc.gameSettings.thirdPersonView == 2);
            this.mc.mcProfiler.endStartSection("frustrum");
            ClippingHelperImpl.getInstance();

            if (this.mc.gameSettings.renderDistance < 2)
            {
                this.setupFog(-1, par1);
                this.mc.mcProfiler.endStartSection("sky");
                renderglobal.renderSky(par1);
            }

            GL11.glEnable(GL11.GL_FOG);
            this.setupFog(1, par1);

            if (this.mc.gameSettings.ambientOcclusion != 0)
            {
                GL11.glShadeModel(GL11.GL_SMOOTH);
            }

            this.mc.mcProfiler.endStartSection("culling");
            Frustrum frustrum = new Frustrum();
            frustrum.setPosition(d0, d1, d2);
            this.mc.renderGlobal.clipRenderersByFrustum(frustrum, par1);

            if (j == 0)
            {
                this.mc.mcProfiler.endStartSection("updatechunks");

                while (!this.mc.renderGlobal.updateRenderers(entityliving, false) && par2 != 0L)
                {
                    long k = par2 - System.nanoTime();

                    if (k < 0L || k > 1000000000L)
                    {
                        break;
                    }
                }
            }

            if (entityliving.posY < 128.0D)
            {
                this.renderCloudsCheck(renderglobal, par1);
            }

            this.mc.mcProfiler.endStartSection("prepareterrain");
            this.setupFog(0, par1);
            GL11.glEnable(GL11.GL_FOG);
            this.mc.renderEngine.bindTexture("/terrain.png");
            RenderHelper.disableStandardItemLighting();
            this.mc.mcProfiler.endStartSection("terrain");
            renderglobal.sortAndRender(entityliving, 0, (double)par1);
            GL11.glShadeModel(GL11.GL_FLAT);
            EntityPlayer entityplayer;

            if (this.debugViewDirection == 0)
            {
                RenderHelper.enableStandardItemLighting();
                this.mc.mcProfiler.endStartSection("entities");
                ForgeHooksClient.setRenderPass(0);
                renderglobal.renderEntities(entityliving.getPosition(par1), frustrum, par1);
                ForgeHooksClient.setRenderPass(-1);

                if (this.mc.objectMouseOver != null && entityliving.isInsideOfMaterial(Material.water) && entityliving instanceof EntityPlayer && !this.mc.gameSettings.hideGUI)
                {
                    entityplayer = (EntityPlayer)entityliving;
                    GL11.glDisable(GL11.GL_ALPHA_TEST);
                    this.mc.mcProfiler.endStartSection("outline");
                    if (!ForgeHooksClient.onDrawBlockHighlight(renderglobal, entityplayer, mc.objectMouseOver, 0, entityplayer.inventory.getCurrentItem(), par1))
                    {
                        renderglobal.drawBlockBreaking(entityplayer, this.mc.objectMouseOver, 0, entityplayer.inventory.getCurrentItem(), par1);
                        renderglobal.drawSelectionBox(entityplayer, this.mc.objectMouseOver, 0, entityplayer.inventory.getCurrentItem(), par1);
                    }
                    GL11.glEnable(GL11.GL_ALPHA_TEST);
                }
            }

            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(true);
            this.setupFog(0, par1);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
            this.mc.renderEngine.bindTexture("/terrain.png");

            if (this.mc.gameSettings.fancyGraphics)
            {
                this.mc.mcProfiler.endStartSection("water");

                if (this.mc.gameSettings.ambientOcclusion != 0)
                {
                    GL11.glShadeModel(GL11.GL_SMOOTH);
                }

                GL11.glColorMask(false, false, false, false);
                int l = renderglobal.sortAndRender(entityliving, 1, (double)par1);

                if (this.mc.gameSettings.anaglyph)
                {
                    if (anaglyphField == 0)
                    {
                        GL11.glColorMask(false, true, true, true);
                    }
                    else
                    {
                        GL11.glColorMask(true, false, false, true);
                    }
                }
                else
                {
                    GL11.glColorMask(true, true, true, true);
                }

                if (l > 0)
                {
                    renderglobal.renderAllRenderLists(1, (double)par1);
                }

                GL11.glShadeModel(GL11.GL_FLAT);
            }
            else
            {
                this.mc.mcProfiler.endStartSection("water");
                renderglobal.sortAndRender(entityliving, 1, (double)par1);
            }

            RenderHelper.enableStandardItemLighting();
            this.mc.mcProfiler.endStartSection("entities");
            ForgeHooksClient.setRenderPass(1);
            renderglobal.renderEntities(entityliving.getPosition(par1), frustrum, par1);
            ForgeHooksClient.setRenderPass(-1);
            RenderHelper.disableStandardItemLighting();

            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_BLEND);

            if (this.cameraZoom == 1.0D && entityliving instanceof EntityPlayer && !this.mc.gameSettings.hideGUI && this.mc.objectMouseOver != null && !entityliving.isInsideOfMaterial(Material.water))
            {
                entityplayer = (EntityPlayer)entityliving;
                GL11.glDisable(GL11.GL_ALPHA_TEST);
                this.mc.mcProfiler.endStartSection("outline");
                if (!ForgeHooksClient.onDrawBlockHighlight(renderglobal, entityplayer, mc.objectMouseOver, 0, entityplayer.inventory.getCurrentItem(), par1))
                {
                    renderglobal.drawBlockBreaking(entityplayer, this.mc.objectMouseOver, 0, entityplayer.inventory.getCurrentItem(), par1);
                    renderglobal.drawSelectionBox(entityplayer, this.mc.objectMouseOver, 0, entityplayer.inventory.getCurrentItem(), par1);
                }
                GL11.glEnable(GL11.GL_ALPHA_TEST);
            }

            this.mc.mcProfiler.endStartSection("destroyProgress");
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            renderglobal.drawBlockDamageTexture(Tessellator.instance, entityliving, par1);
            GL11.glDisable(GL11.GL_BLEND);
            this.mc.mcProfiler.endStartSection("weather");
            this.renderRainSnow(par1);
            GL11.glDisable(GL11.GL_FOG);

            if (entityliving.posY >= 128.0D)
            {
                this.renderCloudsCheck(renderglobal, par1);
            }

            //Forge: Moved section from above, now particles are the last thing to render.
            this.enableLightmap((double)par1);
            this.mc.mcProfiler.endStartSection("litParticles");
            effectrenderer.renderLitParticles(entityliving, par1);
            RenderHelper.disableStandardItemLighting();
            this.setupFog(0, par1);
            this.mc.mcProfiler.endStartSection("particles");
            effectrenderer.renderParticles(entityliving, par1);
            this.disableLightmap((double)par1);

            this.mc.mcProfiler.endStartSection("FRenderLast");
            ForgeHooksClient.dispatchRenderLast(renderglobal, par1);

            this.mc.mcProfiler.endStartSection("hand");

            if (this.cameraZoom == 1.0D)
            {
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
                this.renderHand(par1, j);
            }

            if (!this.mc.gameSettings.anaglyph)
            {
                this.mc.mcProfiler.endSection();
                return;
            }
        }

        GL11.glColorMask(true, true, true, false);
        this.mc.mcProfiler.endSection();
    }

    /**
     * Render clouds if enabled
     */
    private void renderCloudsCheck(RenderGlobal par1RenderGlobal, float par2)
    {
        if (this.mc.gameSettings.shouldRenderClouds())
        {
            this.mc.mcProfiler.endStartSection("clouds");
            GL11.glPushMatrix();
            this.setupFog(0, par2);
            GL11.glEnable(GL11.GL_FOG);
            par1RenderGlobal.renderClouds(par2);
            GL11.glDisable(GL11.GL_FOG);
            this.setupFog(1, par2);
            GL11.glPopMatrix();
        }
    }

    private void addRainParticles()
    {
        float f = this.mc.theWorld.getRainStrength(1.0F);

        if (!this.mc.gameSettings.fancyGraphics)
        {
            f /= 2.0F;
        }

        if (f != 0.0F)
        {
            this.random.setSeed((long)this.rendererUpdateCount * 312987231L);
            EntityLiving entityliving = this.mc.renderViewEntity;
            WorldClient worldclient = this.mc.theWorld;
            int i = MathHelper.floor_double(entityliving.posX);
            int j = MathHelper.floor_double(entityliving.posY);
            int k = MathHelper.floor_double(entityliving.posZ);
            byte b0 = 10;
            double d0 = 0.0D;
            double d1 = 0.0D;
            double d2 = 0.0D;
            int l = 0;
            int i1 = (int)(100.0F * f * f);

            if (this.mc.gameSettings.particleSetting == 1)
            {
                i1 >>= 1;
            }
            else if (this.mc.gameSettings.particleSetting == 2)
            {
                i1 = 0;
            }

            for (int j1 = 0; j1 < i1; ++j1)
            {
                int k1 = i + this.random.nextInt(b0) - this.random.nextInt(b0);
                int l1 = k + this.random.nextInt(b0) - this.random.nextInt(b0);
                int i2 = worldclient.getPrecipitationHeight(k1, l1);
                int j2 = worldclient.getBlockId(k1, i2 - 1, l1);
                BiomeGenBase biomegenbase = worldclient.getBiomeGenForCoords(k1, l1);

                if (i2 <= j + b0 && i2 >= j - b0 && biomegenbase.canSpawnLightningBolt() && biomegenbase.getFloatTemperature() >= 0.2F)
                {
                    float f1 = this.random.nextFloat();
                    float f2 = this.random.nextFloat();

                    if (j2 > 0)
                    {
                        if (Block.blocksList[j2].blockMaterial == Material.lava)
                        {
                            this.mc.effectRenderer.addEffect(new EntitySmokeFX(worldclient, (double)((float)k1 + f1), (double)((float)i2 + 0.1F) - Block.blocksList[j2].getBlockBoundsMinY(), (double)((float)l1 + f2), 0.0D, 0.0D, 0.0D));
                        }
                        else
                        {
                            ++l;

                            if (this.random.nextInt(l) == 0)
                            {
                                d0 = (double)((float)k1 + f1);
                                d1 = (double)((float)i2 + 0.1F) - Block.blocksList[j2].getBlockBoundsMinY();
                                d2 = (double)((float)l1 + f2);
                            }

                            this.mc.effectRenderer.addEffect(new EntityRainFX(worldclient, (double)((float)k1 + f1), (double)((float)i2 + 0.1F) - Block.blocksList[j2].getBlockBoundsMinY(), (double)((float)l1 + f2)));
                        }
                    }
                }
            }

            if (l > 0 && this.random.nextInt(3) < this.rainSoundCounter++)
            {
                this.rainSoundCounter = 0;

                if (d1 > entityliving.posY + 1.0D && worldclient.getPrecipitationHeight(MathHelper.floor_double(entityliving.posX), MathHelper.floor_double(entityliving.posZ)) > MathHelper.floor_double(entityliving.posY))
                {
                    this.mc.theWorld.playSound(d0, d1, d2, "ambient.weather.rain", 0.1F, 0.5F, false);
                }
                else
                {
                    this.mc.theWorld.playSound(d0, d1, d2, "ambient.weather.rain", 0.2F, 1.0F, false);
                }
            }
        }
    }

    /**
     * Render rain and snow
     */
    protected void renderRainSnow(float par1)
    {
        float f1 = this.mc.theWorld.getRainStrength(par1);

        if (f1 > 0.0F)
        {
            this.enableLightmap((double)par1);

            if (this.rainXCoords == null)
            {
                this.rainXCoords = new float[1024];
                this.rainYCoords = new float[1024];

                for (int i = 0; i < 32; ++i)
                {
                    for (int j = 0; j < 32; ++j)
                    {
                        float f2 = (float)(j - 16);
                        float f3 = (float)(i - 16);
                        float f4 = MathHelper.sqrt_float(f2 * f2 + f3 * f3);
                        this.rainXCoords[i << 5 | j] = -f3 / f4;
                        this.rainYCoords[i << 5 | j] = f2 / f4;
                    }
                }
            }

            EntityLiving entityliving = this.mc.renderViewEntity;
            WorldClient worldclient = this.mc.theWorld;
            int k = MathHelper.floor_double(entityliving.posX);
            int l = MathHelper.floor_double(entityliving.posY);
            int i1 = MathHelper.floor_double(entityliving.posZ);
            Tessellator tessellator = Tessellator.instance;
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.01F);
            this.mc.renderEngine.bindTexture("/environment/snow.png");
            double d0 = entityliving.lastTickPosX + (entityliving.posX - entityliving.lastTickPosX) * (double)par1;
            double d1 = entityliving.lastTickPosY + (entityliving.posY - entityliving.lastTickPosY) * (double)par1;
            double d2 = entityliving.lastTickPosZ + (entityliving.posZ - entityliving.lastTickPosZ) * (double)par1;
            int j1 = MathHelper.floor_double(d1);
            byte b0 = 5;

            if (this.mc.gameSettings.fancyGraphics)
            {
                b0 = 10;
            }

            boolean flag = false;
            byte b1 = -1;
            float f5 = (float)this.rendererUpdateCount + par1;

            if (this.mc.gameSettings.fancyGraphics)
            {
                b0 = 10;
            }

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            flag = false;

            for (int k1 = i1 - b0; k1 <= i1 + b0; ++k1)
            {
                for (int l1 = k - b0; l1 <= k + b0; ++l1)
                {
                    int i2 = (k1 - i1 + 16) * 32 + l1 - k + 16;
                    float f6 = this.rainXCoords[i2] * 0.5F;
                    float f7 = this.rainYCoords[i2] * 0.5F;
                    BiomeGenBase biomegenbase = worldclient.getBiomeGenForCoords(l1, k1);

                    if (biomegenbase.canSpawnLightningBolt() || biomegenbase.getEnableSnow())
                    {
                        int j2 = worldclient.getPrecipitationHeight(l1, k1);
                        int k2 = l - b0;
                        int l2 = l + b0;

                        if (k2 < j2)
                        {
                            k2 = j2;
                        }

                        if (l2 < j2)
                        {
                            l2 = j2;
                        }

                        float f8 = 1.0F;
                        int i3 = j2;

                        if (j2 < j1)
                        {
                            i3 = j1;
                        }

                        if (k2 != l2)
                        {
                            this.random.setSeed((long)(l1 * l1 * 3121 + l1 * 45238971 ^ k1 * k1 * 418711 + k1 * 13761));
                            float f9 = biomegenbase.getFloatTemperature();
                            float f10;
                            double d3;

                            if (worldclient.getWorldChunkManager().getTemperatureAtHeight(f9, j2) >= 0.15F)
                            {
                                if (b1 != 0)
                                {
                                    if (b1 >= 0)
                                    {
                                        tessellator.draw();
                                    }

                                    b1 = 0;
                                    this.mc.renderEngine.bindTexture("/environment/rain.png");
                                    tessellator.startDrawingQuads();
                                }

                                f10 = ((float)(this.rendererUpdateCount + l1 * l1 * 3121 + l1 * 45238971 + k1 * k1 * 418711 + k1 * 13761 & 31) + par1) / 32.0F * (3.0F + this.random.nextFloat());
                                double d4 = (double)((float)l1 + 0.5F) - entityliving.posX;
                                d3 = (double)((float)k1 + 0.5F) - entityliving.posZ;
                                float f11 = MathHelper.sqrt_double(d4 * d4 + d3 * d3) / (float)b0;
                                float f12 = 1.0F;
                                tessellator.setBrightness(worldclient.getLightBrightnessForSkyBlocks(l1, i3, k1, 0));
                                tessellator.setColorRGBA_F(f12, f12, f12, ((1.0F - f11 * f11) * 0.5F + 0.5F) * f1);
                                tessellator.setTranslation(-d0 * 1.0D, -d1 * 1.0D, -d2 * 1.0D);
                                tessellator.addVertexWithUV((double)((float)l1 - f6) + 0.5D, (double)k2, (double)((float)k1 - f7) + 0.5D, (double)(0.0F * f8), (double)((float)k2 * f8 / 4.0F + f10 * f8));
                                tessellator.addVertexWithUV((double)((float)l1 + f6) + 0.5D, (double)k2, (double)((float)k1 + f7) + 0.5D, (double)(1.0F * f8), (double)((float)k2 * f8 / 4.0F + f10 * f8));
                                tessellator.addVertexWithUV((double)((float)l1 + f6) + 0.5D, (double)l2, (double)((float)k1 + f7) + 0.5D, (double)(1.0F * f8), (double)((float)l2 * f8 / 4.0F + f10 * f8));
                                tessellator.addVertexWithUV((double)((float)l1 - f6) + 0.5D, (double)l2, (double)((float)k1 - f7) + 0.5D, (double)(0.0F * f8), (double)((float)l2 * f8 / 4.0F + f10 * f8));
                                tessellator.setTranslation(0.0D, 0.0D, 0.0D);
                            }
                            else
                            {
                                if (b1 != 1)
                                {
                                    if (b1 >= 0)
                                    {
                                        tessellator.draw();
                                    }

                                    b1 = 1;
                                    this.mc.renderEngine.bindTexture("/environment/snow.png");
                                    tessellator.startDrawingQuads();
                                }

                                f10 = ((float)(this.rendererUpdateCount & 511) + par1) / 512.0F;
                                float f13 = this.random.nextFloat() + f5 * 0.01F * (float)this.random.nextGaussian();
                                float f14 = this.random.nextFloat() + f5 * (float)this.random.nextGaussian() * 0.001F;
                                d3 = (double)((float)l1 + 0.5F) - entityliving.posX;
                                double d5 = (double)((float)k1 + 0.5F) - entityliving.posZ;
                                float f15 = MathHelper.sqrt_double(d3 * d3 + d5 * d5) / (float)b0;
                                float f16 = 1.0F;
                                tessellator.setBrightness((worldclient.getLightBrightnessForSkyBlocks(l1, i3, k1, 0) * 3 + 15728880) / 4);
                                tessellator.setColorRGBA_F(f16, f16, f16, ((1.0F - f15 * f15) * 0.3F + 0.5F) * f1);
                                tessellator.setTranslation(-d0 * 1.0D, -d1 * 1.0D, -d2 * 1.0D);
                                tessellator.addVertexWithUV((double)((float)l1 - f6) + 0.5D, (double)k2, (double)((float)k1 - f7) + 0.5D, (double)(0.0F * f8 + f13), (double)((float)k2 * f8 / 4.0F + f10 * f8 + f14));
                                tessellator.addVertexWithUV((double)((float)l1 + f6) + 0.5D, (double)k2, (double)((float)k1 + f7) + 0.5D, (double)(1.0F * f8 + f13), (double)((float)k2 * f8 / 4.0F + f10 * f8 + f14));
                                tessellator.addVertexWithUV((double)((float)l1 + f6) + 0.5D, (double)l2, (double)((float)k1 + f7) + 0.5D, (double)(1.0F * f8 + f13), (double)((float)l2 * f8 / 4.0F + f10 * f8 + f14));
                                tessellator.addVertexWithUV((double)((float)l1 - f6) + 0.5D, (double)l2, (double)((float)k1 - f7) + 0.5D, (double)(0.0F * f8 + f13), (double)((float)l2 * f8 / 4.0F + f10 * f8 + f14));
                                tessellator.setTranslation(0.0D, 0.0D, 0.0D);
                            }
                        }
                    }
                }
            }

            if (b1 >= 0)
            {
                tessellator.draw();
            }

            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            this.disableLightmap((double)par1);
        }
    }

    /**
     * Setup orthogonal projection for rendering GUI screen overlays
     */
    public void setupOverlayRendering()
    {
        ScaledResolution scaledresolution = new ScaledResolution(this.mc.gameSettings, this.mc.displayWidth, this.mc.displayHeight);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, scaledresolution.getScaledWidth_double(), scaledresolution.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
    }

    /**
     * calculates fog and calls glClearColor
     */
    private void updateFogColor(float par1)
    {
        WorldClient worldclient = this.mc.theWorld;
        EntityLiving entityliving = this.mc.renderViewEntity;
        float f1 = 1.0F / (float)(4 - this.mc.gameSettings.renderDistance);
        f1 = 1.0F - (float)Math.pow((double)f1, 0.25D);
        Vec3 vec3 = worldclient.getSkyColor(this.mc.renderViewEntity, par1);
        float f2 = (float)vec3.xCoord;
        float f3 = (float)vec3.yCoord;
        float f4 = (float)vec3.zCoord;
        Vec3 vec31 = worldclient.getFogColor(par1);
        this.fogColorRed = (float)vec31.xCoord;
        this.fogColorGreen = (float)vec31.yCoord;
        this.fogColorBlue = (float)vec31.zCoord;
        float f5;

        if (this.mc.gameSettings.renderDistance < 2)
        {
            Vec3 vec32 = MathHelper.sin(worldclient.getCelestialAngleRadians(par1)) > 0.0F ? worldclient.getWorldVec3Pool().getVecFromPool(-1.0D, 0.0D, 0.0D) : worldclient.getWorldVec3Pool().getVecFromPool(1.0D, 0.0D, 0.0D);
            f5 = (float)entityliving.getLook(par1).dotProduct(vec32);

            if (f5 < 0.0F)
            {
                f5 = 0.0F;
            }

            if (f5 > 0.0F)
            {
                float[] afloat = worldclient.provider.calcSunriseSunsetColors(worldclient.getCelestialAngle(par1), par1);

                if (afloat != null)
                {
                    f5 *= afloat[3];
                    this.fogColorRed = this.fogColorRed * (1.0F - f5) + afloat[0] * f5;
                    this.fogColorGreen = this.fogColorGreen * (1.0F - f5) + afloat[1] * f5;
                    this.fogColorBlue = this.fogColorBlue * (1.0F - f5) + afloat[2] * f5;
                }
            }
        }

        this.fogColorRed += (f2 - this.fogColorRed) * f1;
        this.fogColorGreen += (f3 - this.fogColorGreen) * f1;
        this.fogColorBlue += (f4 - this.fogColorBlue) * f1;
        float f6 = worldclient.getRainStrength(par1);
        float f7;

        if (f6 > 0.0F)
        {
            f5 = 1.0F - f6 * 0.5F;
            f7 = 1.0F - f6 * 0.4F;
            this.fogColorRed *= f5;
            this.fogColorGreen *= f5;
            this.fogColorBlue *= f7;
        }

        f5 = worldclient.getWeightedThunderStrength(par1);

        if (f5 > 0.0F)
        {
            f7 = 1.0F - f5 * 0.5F;
            this.fogColorRed *= f7;
            this.fogColorGreen *= f7;
            this.fogColorBlue *= f7;
        }

        int i = ActiveRenderInfo.getBlockIdAtEntityViewpoint(this.mc.theWorld, entityliving, par1);

        if (this.cloudFog)
        {
            Vec3 vec33 = worldclient.getCloudColour(par1);
            this.fogColorRed = (float)vec33.xCoord;
            this.fogColorGreen = (float)vec33.yCoord;
            this.fogColorBlue = (float)vec33.zCoord;
        }
        else if (i != 0 && Block.blocksList[i].blockMaterial == Material.water)
        {
            this.fogColorRed = 0.02F;
            this.fogColorGreen = 0.02F;
            this.fogColorBlue = 0.2F;
        }
        else if (i != 0 && Block.blocksList[i].blockMaterial == Material.lava)
        {
            this.fogColorRed = 0.6F;
            this.fogColorGreen = 0.1F;
            this.fogColorBlue = 0.0F;
        }

        float f8 = this.fogColor2 + (this.fogColor1 - this.fogColor2) * par1;
        this.fogColorRed *= f8;
        this.fogColorGreen *= f8;
        this.fogColorBlue *= f8;
        double d0 = (entityliving.lastTickPosY + (entityliving.posY - entityliving.lastTickPosY) * (double)par1) * worldclient.provider.getVoidFogYFactor();

        if (entityliving.isPotionActive(Potion.blindness))
        {
            int j = entityliving.getActivePotionEffect(Potion.blindness).getDuration();

            if (j < 20)
            {
                d0 *= (double)(1.0F - (float)j / 20.0F);
            }
            else
            {
                d0 = 0.0D;
            }
        }

        if (d0 < 1.0D)
        {
            if (d0 < 0.0D)
            {
                d0 = 0.0D;
            }

            d0 *= d0;
            this.fogColorRed = (float)((double)this.fogColorRed * d0);
            this.fogColorGreen = (float)((double)this.fogColorGreen * d0);
            this.fogColorBlue = (float)((double)this.fogColorBlue * d0);
        }

        float f9;

        if (this.field_82831_U > 0.0F)
        {
            f9 = this.field_82832_V + (this.field_82831_U - this.field_82832_V) * par1;
            this.fogColorRed = this.fogColorRed * (1.0F - f9) + this.fogColorRed * 0.7F * f9;
            this.fogColorGreen = this.fogColorGreen * (1.0F - f9) + this.fogColorGreen * 0.6F * f9;
            this.fogColorBlue = this.fogColorBlue * (1.0F - f9) + this.fogColorBlue * 0.6F * f9;
        }

        float f10;

        if (entityliving.isPotionActive(Potion.nightVision))
        {
            f9 = this.getNightVisionBrightness(this.mc.thePlayer, par1);
            f10 = 1.0F / this.fogColorRed;

            if (f10 > 1.0F / this.fogColorGreen)
            {
                f10 = 1.0F / this.fogColorGreen;
            }

            if (f10 > 1.0F / this.fogColorBlue)
            {
                f10 = 1.0F / this.fogColorBlue;
            }

            this.fogColorRed = this.fogColorRed * (1.0F - f9) + this.fogColorRed * f10 * f9;
            this.fogColorGreen = this.fogColorGreen * (1.0F - f9) + this.fogColorGreen * f10 * f9;
            this.fogColorBlue = this.fogColorBlue * (1.0F - f9) + this.fogColorBlue * f10 * f9;
        }

        if (this.mc.gameSettings.anaglyph)
        {
            f9 = (this.fogColorRed * 30.0F + this.fogColorGreen * 59.0F + this.fogColorBlue * 11.0F) / 100.0F;
            f10 = (this.fogColorRed * 30.0F + this.fogColorGreen * 70.0F) / 100.0F;
            float f11 = (this.fogColorRed * 30.0F + this.fogColorBlue * 70.0F) / 100.0F;
            this.fogColorRed = f9;
            this.fogColorGreen = f10;
            this.fogColorBlue = f11;
        }

        GL11.glClearColor(this.fogColorRed, this.fogColorGreen, this.fogColorBlue, 0.0F);
    }

    /**
     * Sets up the fog to be rendered. If the arg passed in is -1 the fog starts at 0 and goes to 80% of far plane
     * distance and is used for sky rendering.
     */
    private void setupFog(int par1, float par2)
    {
        EntityLiving entityliving = this.mc.renderViewEntity;
        boolean flag = false;

        if (entityliving instanceof EntityPlayer)
        {
            flag = ((EntityPlayer)entityliving).capabilities.isCreativeMode;
        }

        if (par1 == 999)
        {
            GL11.glFog(GL11.GL_FOG_COLOR, this.setFogColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
            GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
            GL11.glFogf(GL11.GL_FOG_START, 0.0F);
            GL11.glFogf(GL11.GL_FOG_END, 8.0F);

            if (GLContext.getCapabilities().GL_NV_fog_distance)
            {
                GL11.glFogi(34138, 34139);
            }

            GL11.glFogf(GL11.GL_FOG_START, 0.0F);
        }
        else
        {
            GL11.glFog(GL11.GL_FOG_COLOR, this.setFogColorBuffer(this.fogColorRed, this.fogColorGreen, this.fogColorBlue, 1.0F));
            GL11.glNormal3f(0.0F, -1.0F, 0.0F);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            int j = ActiveRenderInfo.getBlockIdAtEntityViewpoint(this.mc.theWorld, entityliving, par2);
            float f1;

            if (entityliving.isPotionActive(Potion.blindness))
            {
                f1 = 5.0F;
                int k = entityliving.getActivePotionEffect(Potion.blindness).getDuration();

                if (k < 20)
                {
                    f1 = 5.0F + (this.farPlaneDistance - 5.0F) * (1.0F - (float)k / 20.0F);
                }

                GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);

                if (par1 < 0)
                {
                    GL11.glFogf(GL11.GL_FOG_START, 0.0F);
                    GL11.glFogf(GL11.GL_FOG_END, f1 * 0.8F);
                }
                else
                {
                    GL11.glFogf(GL11.GL_FOG_START, f1 * 0.25F);
                    GL11.glFogf(GL11.GL_FOG_END, f1);
                }

                if (GLContext.getCapabilities().GL_NV_fog_distance)
                {
                    GL11.glFogi(34138, 34139);
                }
            }
            else
            {
                float f2;
                float f3;
                float f4;
                float f5;
                float f6;

                if (this.cloudFog)
                {
                    GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
                    GL11.glFogf(GL11.GL_FOG_DENSITY, 0.1F);
                    f1 = 1.0F;
                    f6 = 1.0F;
                    f2 = 1.0F;

                    if (this.mc.gameSettings.anaglyph)
                    {
                        f3 = (f1 * 30.0F + f6 * 59.0F + f2 * 11.0F) / 100.0F;
                        f4 = (f1 * 30.0F + f6 * 70.0F) / 100.0F;
                        f5 = (f1 * 30.0F + f2 * 70.0F) / 100.0F;
                    }
                }
                else if (j > 0 && Block.blocksList[j].blockMaterial == Material.water)
                {
                    GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);

                    if (entityliving.isPotionActive(Potion.waterBreathing))
                    {
                        GL11.glFogf(GL11.GL_FOG_DENSITY, 0.05F);
                    }
                    else
                    {
                        GL11.glFogf(GL11.GL_FOG_DENSITY, 0.1F);
                    }

                    f1 = 0.4F;
                    f6 = 0.4F;
                    f2 = 0.9F;

                    if (this.mc.gameSettings.anaglyph)
                    {
                        f3 = (f1 * 30.0F + f6 * 59.0F + f2 * 11.0F) / 100.0F;
                        f4 = (f1 * 30.0F + f6 * 70.0F) / 100.0F;
                        f5 = (f1 * 30.0F + f2 * 70.0F) / 100.0F;
                    }
                }
                else if (j > 0 && Block.blocksList[j].blockMaterial == Material.lava)
                {
                    GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
                    GL11.glFogf(GL11.GL_FOG_DENSITY, 2.0F);
                    f1 = 0.4F;
                    f6 = 0.3F;
                    f2 = 0.3F;

                    if (this.mc.gameSettings.anaglyph)
                    {
                        f3 = (f1 * 30.0F + f6 * 59.0F + f2 * 11.0F) / 100.0F;
                        f4 = (f1 * 30.0F + f6 * 70.0F) / 100.0F;
                        f5 = (f1 * 30.0F + f2 * 70.0F) / 100.0F;
                    }
                }
                else
                {
                    f1 = this.farPlaneDistance;

                    if (this.mc.theWorld.provider.getWorldHasVoidParticles() && !flag)
                    {
                        double d0 = (double)((entityliving.getBrightnessForRender(par2) & 15728640) >> 20) / 16.0D + (entityliving.lastTickPosY + (entityliving.posY - entityliving.lastTickPosY) * (double)par2 + 4.0D) / 32.0D;

                        if (d0 < 1.0D)
                        {
                            if (d0 < 0.0D)
                            {
                                d0 = 0.0D;
                            }

                            d0 *= d0;
                            f3 = 100.0F * (float)d0;

                            if (f3 < 5.0F)
                            {
                                f3 = 5.0F;
                            }

                            if (f1 > f3)
                            {
                                f1 = f3;
                            }
                        }
                    }

                    GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);

                    if (par1 < 0)
                    {
                        GL11.glFogf(GL11.GL_FOG_START, 0.0F);
                        GL11.glFogf(GL11.GL_FOG_END, f1 * 0.8F);
                    }
                    else
                    {
                        GL11.glFogf(GL11.GL_FOG_START, f1 * 0.25F);
                        GL11.glFogf(GL11.GL_FOG_END, f1);
                    }

                    if (GLContext.getCapabilities().GL_NV_fog_distance)
                    {
                        GL11.glFogi(34138, 34139);
                    }

                    if (this.mc.theWorld.provider.doesXZShowFog((int)entityliving.posX, (int)entityliving.posZ))
                    {
                        GL11.glFogf(GL11.GL_FOG_START, f1 * 0.05F);
                        GL11.glFogf(GL11.GL_FOG_END, Math.min(f1, 192.0F) * 0.5F);
                    }
                }
            }

            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
            GL11.glColorMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT);
        }
    }

    /**
     * Update and return fogColorBuffer with the RGBA values passed as arguments
     */
    private FloatBuffer setFogColorBuffer(float par1, float par2, float par3, float par4)
    {
        this.fogColorBuffer.clear();
        this.fogColorBuffer.put(par1).put(par2).put(par3).put(par4);
        this.fogColorBuffer.flip();
        return this.fogColorBuffer;
    }

    /**
     * Converts performance value (0-2) to FPS (35-200)
     */
    public static int performanceToFps(int par0)
    {
        short short1 = 200;

        if (par0 == 1)
        {
            short1 = 120;
        }

        if (par0 == 2)
        {
            short1 = 35;
        }

        return short1;
    }

    /**
     * Get minecraft reference from the EntityRenderer
     */
    static Minecraft getRendererMinecraft(EntityRenderer par0EntityRenderer)
    {
        return par0EntityRenderer.mc;
    }
}
