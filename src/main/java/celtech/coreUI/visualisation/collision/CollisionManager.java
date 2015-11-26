package celtech.coreUI.visualisation.collision;

import celtech.modelcontrol.ModelContainer;
import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.dispatch.GhostObject;
import com.bulletphysics.collision.dispatch.GhostPairCallback;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.linearmath.MatrixUtil;
import com.bulletphysics.linearmath.QuaternionUtil;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CollisionManager implements CollisionShapeListener
{

    private final Stenographer steno = StenographerFactory.getStenographer(CollisionManager.class.getName());
    private CollisionWorld collisionWorld = null;
    private final Map<ModelContainer, GhostObject> monitoredModels = new HashMap<>();
    private final Map<ModelContainer, Transform> monitoredModelsTransforms = new HashMap<>();
    private final Timer scheduledTickTimer = new Timer(true);
    private final TimerTask tickRun;
    private long lastTickMilliseconds = 0;

    public CollisionManager()
    {
        tickRun = new TimerTask()
        {
            @Override
            public void run()
            {
                collisionTick();
            }
        };

        setupCollisionWorld();

        lastTickMilliseconds = System.currentTimeMillis();
        scheduledTickTimer.scheduleAtFixedRate(tickRun, 0, 250);
    }

    private void setupCollisionWorld()
    {
        DefaultCollisionConfiguration collisionConfiguration
                = new DefaultCollisionConfiguration();
        // calculates exact collision given a list possible colliding pairs
        CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);

        // the maximum size of the collision world. Make sure objects stay 
        // within these boundaries. Don't make the world AABB size too large, it
        // will harm simulation quality and performance
        Vector3f worldAabbMin = new Vector3f(-1000, -1000, -1000);
        Vector3f worldAabbMax = new Vector3f(1000, 1000, 1000);
        // maximum number of objects
        final int maxProxies = 1024;
        // Broadphase computes an conservative approximate list of colliding pairs
        BroadphaseInterface broadphase = new AxisSweep3(
                worldAabbMin, worldAabbMax, maxProxies);
        broadphase.getOverlappingPairCache().setInternalGhostPairCallback(new GhostPairCallback());

        // provides discrete rigid body simulation
        collisionWorld = new CollisionWorld(dispatcher, broadphase, collisionConfiguration);
    }

    public void addModel(ModelContainer model)
    {
        if (model.getCollisionShape() != null)
        {
            createGhost(model);
        } else
        {
            CollisionShape potentialCollisionShape = model.addCollisionShapeListener(this);
            if (potentialCollisionShape != null)
            {
                createGhost(model);
            }
        }
    }

    public void removeModel(ModelContainer model)
    {
        destroyGhost(model);
    }

    private void createGhost(ModelContainer model)
    {
        GhostObject ghostOfModel = new GhostObject();
        ghostOfModel.setCollisionShape(model.getCollisionShape());
        ghostOfModel.setCollisionFlags(CollisionFlags.NO_CONTACT_RESPONSE);
        collisionWorld.addCollisionObject(ghostOfModel);

        addModelTransformListeners(model);

        monitoredModels.put(model, ghostOfModel);
        Matrix3f rotation = new Matrix3f();
        rotation.setIdentity();
        monitoredModelsTransforms.put(model, new Transform(rotation));
    }

    private void destroyGhost(ModelContainer model)
    {
        removeAllModelListeners(model);

        collisionWorld.removeCollisionObject(monitoredModels.get(model));

        monitoredModels.remove(model);
        monitoredModelsTransforms.remove(model);
    }

    @Override
    public void collisionShapeAvailable(ModelContainer model)
    {
        createGhost(model);
    }

    private void addModelTransformListeners(ModelContainer model)
    {
    }

    private void removeModelTransformListeners(ModelContainer model)
    {
    }

    private void removeAllModelListeners(ModelContainer model)
    {
        model.removeCollisionShapeListener(this);
        removeModelTransformListeners(model);
    }

    private void collisionTick()
    {
        long timeNowMilliseconds = System.currentTimeMillis();
//        steno.info("Tock");
        collisionWorld.performDiscreteCollisionDetection();

//        int numManifolds = collisionWorld.getDispatcher().getNumManifolds();
//        for (int i = 0; i < numManifolds; i++)
//        {
//            PersistentManifold contactManifold = collisionWorld.getDispatcher().getManifoldByIndexInternal(i);
//            CollisionObject objA = (CollisionObject) contactManifold.getBody0();
//            CollisionObject objB = (CollisionObject) contactManifold.getBody1();
//
//            int numContacts = contactManifold.getNumContacts();
//            for (int j = 0; j < numContacts; j++)
//            {
//                ManifoldPoint pt = contactManifold.getContactPoint(j);
//                if (pt.getDistance() < 0.f)
//                {
//                    steno.info("Collision!!" + objA + " : " + objB);
//                }
//            }
//        }

//        for (Entry<ModelContainer, GhostObject> monitoredModelEntry : monitoredModels.entrySet())
//        {
//            Transform a = new Transform();
//            monitoredModelEntry.getValue().getWorldTransform(a);
//            steno.info(a.origin.toString() + ":" + a.basis);
//            ObjectArrayList<CollisionObject> collisionPairs = monitoredModelEntry.getValue().getOverlappingPairs();
//
//            int numPairs = collisionPairs.size();
//            
//            List<PersistentManifold> manifoldArray = new ArrayList<>();

//            for (int i = 0; i < numPairs; ++i)
//            {
////                manifoldArray.clear();
//                CollisionObject pair = collisionPairs.get(i);
//                pair.
//
//                OverlappingPairCache pairCache = collisionWorld.getPairCache().findPair()
//                btBroadphasePair * collisionPair
//                        = dynamicsWorld -> getPairCache()
//                ->findPair(
//                        pair.m_pProxy0, pair.m_pProxy1);
//
//                if (!collisionPair)
//                {
//                    continue;
//                }
//
//                if (collisionPair -> m_algorithm)
//                {
//                    collisionPair -> m_algorithm -> getAllContactManifolds(manifoldArray);
//                }
//
//                for (int j = 0; j < manifoldArray.size(); j++)
//                {
//                    btPersistentManifold * manifold = manifoldArray[j];
//
//                    bool isFirstBody = manifold -> getBody0() == ghostObject;
//
//                    btScalar direction = isFirstBody ? btScalar(-1.0) : btScalar(1.0);
//
//                    for (int p = 0; p < manifold -> getNumContacts(); ++p)
//                    {
//                        const
//                        btManifoldPoint & pt = manifold -> getContactPoint(p);
//
//                        if (pt.getDistance() < 0.f)
//                        {
//                            const
//                            btVector3 & ptA = pt.getPositionWorldOnA();
//                            const
//                            btVector3 & ptB = pt.getPositionWorldOnB();
//                            const
//                            btVector3 & normalOnB = pt.m_normalWorldOnB;
//
//                            // handle collisions here
//                        }
//                    }
//                }
//            }
//
//            if (collidedObjects.size() > 0)
//            {
//                steno.info("Collision!!" + monitoredModelEntry.getKey());
//            }
//        }
        lastTickMilliseconds = timeNowMilliseconds;
    }

    public void modelsTransformed(Set<ModelContainer> modelContainers)
    {
        for (ModelContainer model : modelContainers)
        {
//            Transform worldTxform = new Transform();
//            worldTxform.transform(new Vector3f((float) model.getTransformedCentreX(), (float) model.getTransformedCentreY(), (float) model.getTransformedCentreZ()));
            GhostObject ghost = monitoredModels.get(model);
            if (ghost != null)
            {
//                monitoredModelsTransforms.get(model).origin.set((float) model.getTransformedCentreX(), (float) model.getTransformedCentreY(), (float) model.getTransformedCentreZ());
                monitoredModelsTransforms.get(model).origin.set((float) model.getTransformedCentreX(), 0, (float) model.getTransformedCentreZ());
//                ghost.
                ghost.setWorldTransform(monitoredModelsTransforms.get(model));
            }
        }
    }
}
